using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class VerificationService : IVerificationService
{
    private readonly IMongoCollection<EnergyReservation> _reservations;

    public VerificationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
    }

    public async Task<ReservationResponseDto> VerifyAndFinalizeAsync(VerifyQrDto request)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.QrCode, request.QrCode);
        var reservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        if (reservation is null)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Invalid QR code"
            };
        }

        if (reservation.Status != "Approved")
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not in approved state"
            };
        }

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Completed")
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        await _reservations.UpdateOneAsync(filter, update);

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Transfer finalized",
            ReservationId = reservation.Id,
            Status = "Completed",
            QrCode = reservation.QrCode
        };
    }

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
