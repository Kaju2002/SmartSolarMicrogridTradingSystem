/*
 * File: CreateReservationDto.cs
 * Description: Create booking request
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
namespace SolarGrid.API.DTOs;

public class CreateReservationDto
{
    public string ProsumerNic { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public DateTime ReservationDateTime { get; set; }
}
