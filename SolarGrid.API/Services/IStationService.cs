/*
 * File: IStationService.cs
 * Description: Station service contract
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IStationService
{
    // Create station
    Task<StationResponseDto> CreateStationAsync(CreateStationDto request);
    // List stations
    Task<List<SolarStationInfo>> GetAllStationsAsync();
    // Update station
    Task<StationResponseDto> UpdateStationAsync(string stationId, UpdateStationDto request);
    // Deactivate station
    Task<StationResponseDto> DeactivateStationAsync(string stationId);
    // Nearby stations
    Task<List<StationResponseDto>> GetNearbyStationsAsync(double latitude, double longitude, double radiusKm);
}
