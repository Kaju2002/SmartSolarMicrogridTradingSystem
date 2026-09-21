namespace SolarGrid.API.DTOs;

public class StationResponseDto
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public string? StationId { get; set; }
    public string? StationName { get; set; }
    public double? Latitude { get; set; }
    public double? Longitude { get; set; }
    public double? DistanceKm { get; set; }
    // nearby search only
}
