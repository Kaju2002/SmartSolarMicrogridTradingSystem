/*
 * File: ReservationResponseDto.cs
 * Description: Booking action response
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
namespace SolarGrid.API.DTOs;

public class ReservationResponseDto
{
    public bool Success { get; set; }

    public string Message { get; set; } = string.Empty;

    public string? ReservationId { get; set; }

    public string? Status { get; set; }

    public DateTime? ReservationDateTime { get; set; }

    public double? RequestedKWh { get; set; }

    public double? EstimatedCost { get; set; }
    // LKR

    public string? QrCode { get; set; }
}
