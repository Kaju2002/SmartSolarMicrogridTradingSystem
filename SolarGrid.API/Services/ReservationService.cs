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

    // Setup Reservations collection
    public ReservationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
    }

    // Create booking if date is within 7 days
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

        var newReservation = new EnergyReservation
        {
            ProsumerNic = request.ProsumerNic,
            StationId = request.StationId,
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

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.ReservationDateTime, request.NewReservationDateTime)
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        await _reservations.UpdateOneAsync(filter, update);

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation updated",
            ReservationId = existingReservation.Id,
            Status = existingReservation.Status,
            ReservationDateTime = request.NewReservationDateTime
        };
    }

    // Cancel booking if more than 12 hours left
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
