/*
 * File: IVerificationService.cs
 * Description: Verification and dashboard service contract
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IVerificationService
{
    // Finalize by QR; operatorId null = Backoffice (any station)
    Task<ReservationResponseDto> VerifyAndFinalizeAsync(VerifyQrDto request, string? operatorId);
    // Dashboard counts
    Task<DashboardSummaryDto> GetDashboardSummaryAsync(string prosumerNic);
    // Booking history
    Task<List<EnergyReservation>> GetBookingHistoryAsync(string prosumerNic);
    // Search bookings
    Task<List<EnergyReservation>> SearchBookingsAsync(string prosumerNic, string? query);
}
