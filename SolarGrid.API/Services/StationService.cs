/*
 * File: StationService.cs
 * Description: Station create, update, deactivate and nearby search
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class StationService : IStationService
{
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<User> _users;

    // Setup Stations, Reservations and Users collections
    public StationService(MongoDbContext dbContext)
    {
        _stations = dbContext.Database.GetCollection<SolarStationInfo>("Stations");
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
        _users = dbContext.Database.GetCollection<User>("Users");
    }

    // Add new solar station
    public async Task<StationResponseDto> CreateStationAsync(CreateStationDto request)
    {
        var operatorCheck = await ValidateOperatorAsync(request.AssignedOperatorId);
        if (!operatorCheck.Ok)
        {
            return new StationResponseDto
            {
                Success = false,
                Message = operatorCheck.Message
            };
        }

        var station = new SolarStationInfo
        {
            StationName = request.StationName,
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            CapacityKWh = request.CapacityKWh,
            BatterySlots = request.BatterySlots,
            AvailableSlots = request.BatterySlots,
            OpenTime = request.OpenTime,
            CloseTime = request.CloseTime,
            CreatedBy = request.CreatedBy,
            AssignedOperatorId = operatorCheck.OperatorId,
            Status = "Active",
            CreatedAt = DateTime.UtcNow
        };

        await _stations.InsertOneAsync(station);

        return new StationResponseDto
        {
            Success = true,
            Message = "Station created",
            StationId = station.Id,
            StationName = station.StationName
        };
    }

    // Return all stations
    public async Task<List<SolarStationInfo>> GetAllStationsAsync()
    {
        return await _stations.Find(_ => true).ToListAsync();
    }

    // Partial update for capacity, slots, hours or assigned operator
    public async Task<StationResponseDto> UpdateStationAsync(string stationId, UpdateStationDto request)
    {
        var filter = Builders<SolarStationInfo>.Filter.Eq(s => s.Id, stationId);
        var station = await _stations.Find(filter).FirstOrDefaultAsync();

        if (station is null)
        {
            return new StationResponseDto
            {
                Success = false,
                Message = "Station not found"
            };
        }

        var updates = new List<UpdateDefinition<SolarStationInfo>>();

        if (request.CapacityKWh.HasValue)
            updates.Add(Builders<SolarStationInfo>.Update.Set(s => s.CapacityKWh, request.CapacityKWh.Value));

        if (request.BatterySlots.HasValue)
            updates.Add(Builders<SolarStationInfo>.Update.Set(s => s.BatterySlots, request.BatterySlots.Value));

        if (request.OpenTime is not null)
            updates.Add(Builders<SolarStationInfo>.Update.Set(s => s.OpenTime, request.OpenTime));

        if (request.CloseTime is not null)
            updates.Add(Builders<SolarStationInfo>.Update.Set(s => s.CloseTime, request.CloseTime));

        if (request.UpdateAssignedOperator)
        {
            var operatorCheck = await ValidateOperatorAsync(request.AssignedOperatorId);
            if (!operatorCheck.Ok)
            {
                return new StationResponseDto
                {
                    Success = false,
                    Message = operatorCheck.Message
                };
            }

            updates.Add(Builders<SolarStationInfo>.Update.Set(
                s => s.AssignedOperatorId,
                operatorCheck.OperatorId));
        }

        updates.Add(Builders<SolarStationInfo>.Update.Set(s => s.UpdatedAt, DateTime.UtcNow));

        await _stations.UpdateOneAsync(filter, Builders<SolarStationInfo>.Update.Combine(updates));

        return new StationResponseDto
        {
            Success = true,
            Message = "Station updated",
            StationId = station.Id,
            StationName = station.StationName,
            Latitude = station.Latitude,
            Longitude = station.Longitude
        };
    }

    // Ensure assigned user is an Active Grid Operator (or clear)
    private async Task<(bool Ok, string Message, string? OperatorId)> ValidateOperatorAsync(string? operatorId)
    {
        if (string.IsNullOrWhiteSpace(operatorId))
        {
            return (true, string.Empty, null);
        }

        var user = await _users.Find(u => u.Id == operatorId).FirstOrDefaultAsync();
        if (user is null)
        {
            return (false, "Assigned operator not found", null);
        }

        if (!string.Equals(user.UserType, "GridOperator", StringComparison.OrdinalIgnoreCase))
        {
            return (false, "Assigned user must be a Grid Operator", null);
        }

        if (!string.Equals(user.Status, "Active", StringComparison.OrdinalIgnoreCase))
        {
            return (false, "Assigned Grid Operator must be Active", null);
        }

        return (true, string.Empty, user.Id);
    }

    // Deactivate only if no pending/approved bookings
    public async Task<StationResponseDto> DeactivateStationAsync(string stationId)
    {
        var filter = Builders<SolarStationInfo>.Filter.Eq(s => s.Id, stationId);
        var station = await _stations.Find(filter).FirstOrDefaultAsync();

        if (station is null)
        {
            return new StationResponseDto
            {
                Success = false,
                Message = "Station not found"
            };
        }

        var activeCheck = Builders<EnergyReservation>.Filter.And(
            Builders<EnergyReservation>.Filter.Eq(r => r.StationId, stationId),
            Builders<EnergyReservation>.Filter.In(r => r.Status, new[] { "Pending", "Approved" })
        );

        var hasActive = await _reservations.Find(activeCheck).AnyAsync();
        if (hasActive)
        {
            return new StationResponseDto
            {
                Success = false,
                Message = "Cannot deactivate — active reservations exist"
            };
        }

        var update = Builders<SolarStationInfo>.Update
            .Set(s => s.Status, "Deactivated")
            .Set(s => s.UpdatedAt, DateTime.UtcNow);

        await _stations.UpdateOneAsync(filter, update);

        return new StationResponseDto
        {
            Success = true,
            Message = "Station deactivated",
            StationId = station.Id,
            StationName = station.StationName
        };
    }

    // Find active stations inside radius using Haversine
    public async Task<List<StationResponseDto>> GetNearbyStationsAsync(double latitude, double longitude, double radiusKm)
    {
        var activeStations = await _stations
            .Find(s => s.Status == "Active")
            .ToListAsync();

        var nearby = activeStations
            .Select(s =>
            {
                var distance = CalculateDistance(latitude, longitude, s.Latitude, s.Longitude);
                return new StationResponseDto
                {
                    Success = true,
                    Message = "Nearby station",
                    StationId = s.Id,
                    StationName = s.StationName,
                    Latitude = s.Latitude,
                    Longitude = s.Longitude,
                    DistanceKm = distance
                };
            })
            .Where(s => s.DistanceKm <= radiusKm)
            .OrderBy(s => s.DistanceKm)
            .ToList();

        return nearby;
    }

    // Haversine distance in km
    private double CalculateDistance(double lat1, double lon1, double lat2, double lon2)
    {
        var R = 6371; // Earth radius in km

        // convert degree difference to radians
        var dLat = (lat2 - lat1) * Math.PI / 180;
        var dLon = (lon2 - lon1) * Math.PI / 180;

        // Haversine formula
        var a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                Math.Cos(lat1 * Math.PI / 180) * Math.Cos(lat2 * Math.PI / 180) *
                Math.Sin(dLon / 2) * Math.Sin(dLon / 2);

        // angular distance
        var c = 2 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1 - a));

        return R * c; // distance in km
    }
}
