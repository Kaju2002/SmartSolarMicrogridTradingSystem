using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IVerificationService
{
    Task<ReservationResponseDto> VerifyAndFinalizeAsync(VerifyQrDto request);
    Task<DashboardSummaryDto> GetDashboardSummaryAsync(string prosumerNic);
    Task<List<EnergyReservation>> GetBookingHistoryAsync(string prosumerNic);
    Task<List<EnergyReservation>> SearchBookingsAsync(string prosumerNic, string? query);
}
