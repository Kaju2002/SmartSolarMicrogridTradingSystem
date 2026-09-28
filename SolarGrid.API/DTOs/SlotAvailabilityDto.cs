/*
 * File: SlotAvailabilityDto.cs
 * Description: Bookable hourly slots for one station and day
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 28/09/2026
 */
namespace SolarGrid.API.DTOs;

public class SlotAvailabilityDto
{
    public string StationId { get; set; } = string.Empty;

    public string Date { get; set; } = string.Empty;
    // yyyy-MM-dd, Sri Lanka date

    public string OpenTime { get; set; } = string.Empty;

    public string CloseTime { get; set; } = string.Empty;

    public int SpotsPerSlot { get; set; }
    // one booking per battery slot at the same hour

    public double CapacityKWh { get; set; }
    // shared by all bookings in the same hour

    public double RatePerKwh { get; set; }
    // LKR, for the cost preview

    public double MinKWh { get; set; }
    // smallest request per booking

    public List<BookingSlotDto> Slots { get; set; } = new();
}

public class BookingSlotDto
{
    public DateTime SlotDateTime { get; set; }
    // UTC start, send this back when booking

    public string StartTime { get; set; } = string.Empty;
    // Sri Lanka time, e.g. "09:00"

    public string EndTime { get; set; } = string.Empty;

    public int SpotsLeft { get; set; }

    public double KWhLeft { get; set; }
    // most a new booking can ask for in this hour

    public string Status { get; set; } = "Available";
    // Available | Full | Unavailable (past or beyond 7 days)
}
