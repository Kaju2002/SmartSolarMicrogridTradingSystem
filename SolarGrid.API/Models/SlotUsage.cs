/*
 * File: SlotUsage.cs
 * Description: Booked spots and kWh for one station hour (capacity counter)
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 09/10/2026
 */
using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SolarGrid.API.Models;

public class SlotUsage
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string? Id { get; set; }

    public string StationId { get; set; } = string.Empty;

    public DateTime SlotTime { get; set; }
    // UTC start of the hour

    public int BookedCount { get; set; }

    public double BookedKWh { get; set; }
}
