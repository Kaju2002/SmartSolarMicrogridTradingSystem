using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IStationService
{
    Task<StationResponseDto> CreateStationAsync(CreateStationDto request);
    Task<List<SolarStationInfo>> GetAllStationsAsync();
    Task<StationResponseDto> UpdateStationAsync(string stationId, UpdateStationDto request);
    Task<StationResponseDto> DeactivateStationAsync(string stationId);
    Task<List<StationResponseDto>> GetNearbyStationsAsync(double latitude, double longitude, double radiusKm);
}