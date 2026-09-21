using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IReservationService
{
    Task<ReservationResponseDto> CreateReservationAsync(CreateReservationDto request);
    Task<ReservationResponseDto> UpdateReservationAsync(string reservationId, UpdateReservationDto request);
    Task<ReservationResponseDto> CancelReservationAsync(string reservationId);
    Task<List<EnergyReservation>> GetAllReservationsAsync();
    Task<List<EnergyReservation>> GetReservationsByProsumerAsync(string nic);
    Task<ReservationResponseDto> ApproveReservationAsync(string reservationId, string approvedByUserId);
}