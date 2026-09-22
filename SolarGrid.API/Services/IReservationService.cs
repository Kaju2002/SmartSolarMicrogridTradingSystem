/*
 * File: IReservationService.cs
 * Description: Reservation service contract
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IReservationService
{
    // Create booking
    Task<ReservationResponseDto> CreateReservationAsync(CreateReservationDto request);
    // Update booking
    Task<ReservationResponseDto> UpdateReservationAsync(string reservationId, UpdateReservationDto request);
    // Cancel booking
    Task<ReservationResponseDto> CancelReservationAsync(string reservationId);
    // List all
    Task<List<EnergyReservation>> GetAllReservationsAsync();
    // List by NIC
    Task<List<EnergyReservation>> GetReservationsByProsumerAsync(string nic);
    // Approve booking
    Task<ReservationResponseDto> ApproveReservationAsync(string reservationId, string approvedByUserId);
}
