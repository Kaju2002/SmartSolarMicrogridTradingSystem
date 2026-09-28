/*
 * File: BookingTime.cs
 * Description: Sri Lanka time and slot helpers for bookings
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 28/09/2026
 */
using System.Globalization;

namespace SolarGrid.API.Helpers;

public static class BookingTime
{
    // Sri Lanka is UTC+05:30 all year, no daylight saving
    public static readonly TimeSpan LocalOffset = TimeSpan.FromMinutes(330);

    public static readonly TimeSpan SlotLength = TimeSpan.FromHours(1);

    public const int BookingWindowDays = 7;

    private static readonly string[] HourFormats = { "HH:mm", "H:mm" };

    // Unmarked times are treated as UTC
    public static DateTime ToUtc(DateTime value) => value.Kind switch
    {
        DateTimeKind.Utc => value,
        DateTimeKind.Local => value.ToUniversalTime(),
        _ => DateTime.SpecifyKind(value, DateTimeKind.Utc)
    };

    // UTC instant to Sri Lanka wall-clock time
    public static DateTime ToLocal(DateTime utc) =>
        DateTime.SpecifyKind(ToUtc(utc) + LocalOffset, DateTimeKind.Unspecified);

    // Sri Lanka wall-clock time to UTC
    public static DateTime FromLocal(DateTime local) =>
        DateTime.SpecifyKind(local - LocalOffset, DateTimeKind.Utc);

    // Station hours like "06:00"; null when missing or invalid
    public static TimeOnly? ParseHour(string? value) =>
        TimeOnly.TryParseExact(value?.Trim(), HourFormats, CultureInfo.InvariantCulture,
            DateTimeStyles.None, out var time) ? time : null;

    // Future and no more than 7 days ahead
    public static bool IsWithinWindow(DateTime slotUtc)
    {
        var days = (ToUtc(slotUtc) - DateTime.UtcNow).TotalDays;
        return days >= 0 && days <= BookingWindowDays;
    }
}
