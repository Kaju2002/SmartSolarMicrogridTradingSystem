using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class StationsController : ControllerBase
{
    private readonly IStationService _stationService;

    public StationsController(IStationService stationService)
    {
        _stationService = stationService;
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateStationDto request)
    {
        var result = await _stationService.CreateStationAsync(request);
        return Ok(result);
    }

    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var stations = await _stationService.GetAllStationsAsync();
        return Ok(stations);
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateStationDto request)
    {
        var result = await _stationService.UpdateStationAsync(id, request);
        if (result.Success) return Ok(result);
        return NotFound(result);
    }

    [HttpPut("{id}/deactivate")]
    public async Task<IActionResult> Deactivate(string id)
    {
        var result = await _stationService.DeactivateStationAsync(id);
        if (result.Success) return Ok(result);
        return BadRequest(result);
    }

    [HttpGet("nearby")]
    public async Task<IActionResult> GetNearby([FromQuery] double lat, [FromQuery] double lng, [FromQuery] double radiusKm = 10)
    {
        var stations = await _stationService.GetNearbyStationsAsync(lat, lng, radiusKm);
        return Ok(stations);
    }
}
