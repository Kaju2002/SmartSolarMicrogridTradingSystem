/*
 * File: OperatorReservationDto.cs
 * Description: Booking row for a Grid Operator's stations
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 30/09/2026
 */
namespace SolarGrid.API.DTOs;

public class OperatorReservationDto
{
    public string Id { get; set; } = string.Empty;

    public string ProsumerNic { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public string StationName { get; set; } = string.Empty;

    public DateTime ReservationDateTime { get; set; }

    public double RequestedKWh { get; set; }

    public double EstimatedCost { get; set; }
    // LKR

    public string Status { get; set; } = string.Empty;

    public string? QrCode { get; set; }

    public DateTime CreatedAt { get; set; }

    public DateTime? LastModifiedAt { get; set; }
}
