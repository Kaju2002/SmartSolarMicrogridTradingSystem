/*
 * File: EnergyReservation.cs
 * Description: Energy booking / reservation model
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string ProsumerNic { get; set; } = string.Empty;
    // Users.Nic

    public string StationId { get; set; } = string.Empty;
    // SolarStationInfo id

    public string? BookingSlotId { get; set; }
    // EnergyBookingSlot id

    public DateTime ReservationDateTime { get; set; }
    // slot time, within 7 days

    public string Status { get; set; } = "Pending";
    // Pending | Approved | Completed | Cancelled

    public string? QrCode { get; set; }
    // set when Approved

    public string? ApprovedBy { get; set; }
    // Grid Operator user id

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? LastModifiedAt { get; set; }
    // for 12 hour update/cancel check
}
