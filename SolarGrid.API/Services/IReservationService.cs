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
    // NIC for a Prosumer user id; null for staff
    Task<string?> GetProsumerNicAsync(string userId);
    // Create booking
    Task<ReservationResponseDto> CreateReservationAsync(CreateReservationDto request);
    // Update booking; ownerNic null skips ownership check (staff)
    Task<ReservationResponseDto> UpdateReservationAsync(string reservationId, UpdateReservationDto request, string? ownerNic);
    // Hourly slots for a station on one Sri Lanka date; null if station unknown
    Task<SlotAvailabilityDto?> GetAvailabilityAsync(string stationId, DateOnly date);
    // Cancel booking; ownerNic null skips ownership check (staff)
    Task<ReservationResponseDto> CancelReservationAsync(string reservationId, string? ownerNic);
    // List all
    Task<List<EnergyReservation>> GetAllReservationsAsync();
    // List by NIC
    Task<List<EnergyReservation>> GetReservationsByProsumerAsync(string nic);
    // Approve booking
    Task<ReservationResponseDto> ApproveReservationAsync(string reservationId, string approvedByUserId);
}
