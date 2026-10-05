/*
 * File: VerificationService.cs
 * Description: QR verify and finalize, dashboard counts, history and search
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
using System.Globalization;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class VerificationService : IVerificationService
{
    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly IMongoCollection<User> _users;

    // Setup Reservations, Stations and Users collections
    public VerificationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
        _stations = dbContext.Database.GetCollection<SolarStationInfo>("Stations");
        _users = dbContext.Database.GetCollection<User>("Users");
    }

    // Failed scan response
    private static ReservationResponseDto Fail(string message) =>
        new() { Success = false, Message = message };

    // QR checks shared by verify and finalize: known code, operator's station, Approved, slot day today
    private async Task<(string? Error, EnergyReservation? Reservation, SolarStationInfo? Station)> CheckQrAsync(
        VerifyQrDto request, string? operatorId)
    {
        var qrCode = request.QrCode?.Trim();
        if (string.IsNullOrEmpty(qrCode))
            return ("QR code is required", null, null);

        var reservation = await _reservations.Find(r => r.QrCode == qrCode).FirstOrDefaultAsync();
        if (reservation is null)
            return ("Invalid QR code", null, null);

        var station = ObjectId.TryParse(reservation.StationId, out _)
            ? await _stations.Find(s => s.Id == reservation.StationId).FirstOrDefaultAsync()
            : null;

        if (operatorId is not null && station?.AssignedOperatorId != operatorId)
            return ("This booking is for another station", null, null);

        if (reservation.Status == "Completed")
            return ("This QR code has already been used", null, null);

        if (reservation.Status == "Cancelled")
            return ("This booking was cancelled", null, null);

        if (reservation.Status != "Approved")
            return ("This booking is not approved yet", null, null);

        var slotLocal = BookingTime.ToLocal(reservation.ReservationDateTime);
        var todayLocal = BookingTime.ToLocal(DateTime.UtcNow).Date;
        if (slotLocal.Date != todayLocal)
        {
            var slotText = slotLocal.ToString("dd MMM yyyy, HH:mm", CultureInfo.InvariantCulture);
            return ($"This booking is for {slotText}. Scan it on that day.", null, null);
        }

        return (null, reservation, station);
    }

    // Booking details for a QR, read only, so the operator can check them before finishing
    public async Task<ReservationResponseDto> VerifyQrAsync(VerifyQrDto request, string? operatorId)
    {
        var (error, reservation, station) = await CheckQrAsync(request, operatorId);
        if (error is not null || reservation is null)
            return Fail(error ?? "Invalid QR code");

        var prosumer = await _users.Find(u => u.Nic == reservation.ProsumerNic).FirstOrDefaultAsync();

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Booking verified. Check the details, then confirm the transfer.",
            ReservationId = reservation.Id,
            Status = reservation.Status,
            ReservationDateTime = reservation.ReservationDateTime,
            RequestedKWh = reservation.RequestedKWh,
            EstimatedCost = reservation.EstimatedCost,
            QrCode = reservation.QrCode,
            ProsumerNic = reservation.ProsumerNic,
            ProsumerName = prosumer?.FullName,
            StationName = station?.StationName
        };
    }

    // Complete an Approved booking by QR on its slot day; operatorId null = Backoffice (any station)
    public async Task<ReservationResponseDto> VerifyAndFinalizeAsync(VerifyQrDto request, string? operatorId)
    {
        var (error, reservation, station) = await CheckQrAsync(request, operatorId);
        if (error is not null || reservation is null)
            return Fail(error ?? "Invalid QR code");

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Completed")
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        // Still-Approved filter makes a second scan of the same QR fail
        var stillApproved = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservation.Id)
                            & Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved");
        var completed = await _reservations.FindOneAndUpdateAsync(stillApproved, update);
        if (completed is null)
            return Fail("This QR code has already been used");

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Energy transfer completed",
            ReservationId = reservation.Id,
            Status = "Completed",
            ReservationDateTime = reservation.ReservationDateTime,
            RequestedKWh = reservation.RequestedKWh,
            EstimatedCost = reservation.EstimatedCost,
            QrCode = reservation.QrCode,
            ProsumerNic = reservation.ProsumerNic,
            StationName = station?.StationName
        };
    }

    // Count pending, approved and completed for one NIC
    public async Task<DashboardSummaryDto> GetDashboardSummaryAsync(string prosumerNic)
    {
        var pendingFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, prosumerNic),
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Pending")
        );

        var approvedFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, prosumerNic),
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Approved")
        );

        var completedFilter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, prosumerNic),
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Completed")
        );

        var pendingCount = (int)await _reservations.CountDocumentsAsync(pendingFilter);
        var approvedCount = (int)await _reservations.CountDocumentsAsync(approvedFilter);
        var completedCount = (int)await _reservations.CountDocumentsAsync(completedFilter);

        return new DashboardSummaryDto
        {
            PendingCount = pendingCount,
            ApprovedCount = approvedCount,
            CompletedCount = completedCount
        };
    }

    // Past completed bookings for prosumer
    public async Task<List<EnergyReservation>> GetBookingHistoryAsync(string prosumerNic)
    {
        var filter = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, prosumerNic),
            Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Completed")
        );

        return await _reservations
            .Find(filter)
            .SortByDescending(r => r.ReservationDateTime)
            .ToListAsync();
    }

    // Search bookings by status text for one NIC
    public async Task<List<EnergyReservation>> SearchBookingsAsync(string prosumerNic, string? query)
    {
        var filters = new List<FilterDefinition<EnergyReservation>>
        {
            Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, prosumerNic)
        };

        if (!string.IsNullOrWhiteSpace(query))
        {
            filters.Add(Builders<EnergyReservation>.Filter.Regex(
                r => r.Status,
                new MongoDB.Bson.BsonRegularExpression(query, "i")
            ));
        }

        return await _reservations
            .Find(Builders<EnergyReservation>.Filter.And(filters))
            .ToListAsync();
    }
}
