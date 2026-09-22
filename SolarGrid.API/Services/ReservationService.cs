/*
 * File: ReservationService.cs
 * Description: Booking create, update, cancel and approve
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class ReservationService : IReservationService
{
    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<EnergyBookingSlot> _bookingSlots;

    // Setup Reservations and EnergyBookingSlots collections
    public ReservationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
        _bookingSlots = dbContext.Database.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");
    }

    // Create booking with 7-day rule and prevent double booking
    public async Task<ReservationResponseDto> CreateReservationAsync(CreateReservationDto request)
    {
        var daysDifference = (request.ReservationDateTime - DateTime.UtcNow).TotalDays;
        if (daysDifference < 0 || daysDifference > 7)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation must be within 7 days"
            };
        }

        // Check if this station+time is already booked
        var slotFilter = Builders<EnergyBookingSlot>.Filter.And(
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, request.StationId),
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotDateTime, request.ReservationDateTime),
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotStatus, "Booked")
        );

        var existingSlot = await _bookingSlots.Find(slotFilter).FirstOrDefaultAsync();
        if (existingSlot is not null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "This time slot is already booked"
            };
        }

        // Create booked slot first
        var newSlot = new EnergyBookingSlot
        {
            StationId = request.StationId,
            SlotDateTime = request.ReservationDateTime,
            SlotStatus = "Booked",
            CapacityKWh = 0,
            CreatedAt = DateTime.UtcNow
        };

        await _bookingSlots.InsertOneAsync(newSlot);

        var newReservation = new EnergyReservation
        {
            ProsumerNic = request.ProsumerNic,
            StationId = request.StationId,
            BookingSlotId = newSlot.Id,
            ReservationDateTime = request.ReservationDateTime,
            Status = "Pending",
            CreatedAt = DateTime.UtcNow
        };

        await _reservations.InsertOneAsync(newReservation);

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation created",
            ReservationId = newReservation.Id,
            Status = "Pending",
            ReservationDateTime = newReservation.ReservationDateTime
        };
    }

    // Change slot time if more than 12 hours left
    public async Task<ReservationResponseDto> UpdateReservationAsync(string reservationId, UpdateReservationDto request)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        if (existingReservation is null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not found"
            };
        }

        var hoursUntilSlot = (existingReservation.ReservationDateTime - DateTime.UtcNow).TotalHours;
        if (hoursUntilSlot < 12)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Cannot update within 12 hours of reservation"
            };
        }

        // New time must also be free on this station
        var slotTakenFilter = Builders<EnergyBookingSlot>.Filter.And(
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.StationId, existingReservation.StationId),
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotDateTime, request.NewReservationDateTime),
            Builders<EnergyBookingSlot>.Filter.Eq(s => s.SlotStatus, "Booked"),
            Builders<EnergyBookingSlot>.Filter.Ne(s => s.Id, existingReservation.BookingSlotId)
        );

        var takenSlot = await _bookingSlots.Find(slotTakenFilter).FirstOrDefaultAsync();
        if (takenSlot is not null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "This time slot is already booked"
            };
        }

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.ReservationDateTime, request.NewReservationDateTime)
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        await _reservations.UpdateOneAsync(filter, update);

        // Keep linked slot datetime in sync
        if (existingReservation.BookingSlotId is not null)
        {
            var linkedSlotFilter = Builders<EnergyBookingSlot>.Filter.Eq(s => s.Id, existingReservation.BookingSlotId);
            var slotUpdate = Builders<EnergyBookingSlot>.Update
                .Set(s => s.SlotDateTime, request.NewReservationDateTime)
                .Set(s => s.SlotStatus, "Booked");

            await _bookingSlots.UpdateOneAsync(linkedSlotFilter, slotUpdate);
        }

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation updated",
            ReservationId = existingReservation.Id,
            Status = existingReservation.Status,
            ReservationDateTime = request.NewReservationDateTime
        };
    }

    // Cancel booking and free the linked slot
    public async Task<ReservationResponseDto> CancelReservationAsync(string reservationId)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        if (existingReservation is null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not found"
            };
        }

        var hoursUntilSlot = (existingReservation.ReservationDateTime - DateTime.UtcNow).TotalHours;
        if (hoursUntilSlot < 12)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Cannot cancel within 12 hours of reservation"
            };
        }

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Cancelled")
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        await _reservations.UpdateOneAsync(filter, update);

        // Release linked booking slot
        if (existingReservation.BookingSlotId is not null)
        {
            var slotFilter = Builders<EnergyBookingSlot>.Filter.Eq(s => s.Id, existingReservation.BookingSlotId);
            var slotUpdate = Builders<EnergyBookingSlot>.Update.Set(s => s.SlotStatus, "Available");
            await _bookingSlots.UpdateOneAsync(slotFilter, slotUpdate);
        }

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation cancelled",
            ReservationId = existingReservation.Id,
            Status = "Cancelled"
        };
    }

    // Get all reservations
    public async Task<List<EnergyReservation>> GetAllReservationsAsync()
    {
        return await _reservations.Find(_ => true).ToListAsync();
    }

    // Get reservations for one prosumer by NIC
    public async Task<List<EnergyReservation>> GetReservationsByProsumerAsync(string nic)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, nic);
        return await _reservations.Find(filter).ToListAsync();
    }

    // Approve booking and create QR code string
    public async Task<ReservationResponseDto> ApproveReservationAsync(string reservationId, string approvedByUserId)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        if (existingReservation is null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not found"
            };
        }

        var qrCode = Guid.NewGuid().ToString();

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Approved")
            .Set(r => r.ApprovedBy, approvedByUserId)
            .Set(r => r.QrCode, qrCode)
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        await _reservations.UpdateOneAsync(filter, update);

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation approved",
            ReservationId = existingReservation.Id,
            Status = "Approved",
            QrCode = qrCode
        };
    }
}
