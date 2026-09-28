/*
 * File: EnergyBookingSlot.cs
 * Description: Station time slot availability model
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 22/09/2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class EnergyBookingSlot
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string StationId { get; set; } = string.Empty;

    public DateTime SlotDateTime { get; set; }

    public string SlotStatus { get; set; } = "Available";
    // Available | Booked | Blocked

    public double CapacityKWh { get; set; }
    // kWh held by the booking in this slot

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
