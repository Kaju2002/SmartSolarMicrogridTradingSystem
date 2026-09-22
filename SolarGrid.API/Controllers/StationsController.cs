/*
 * File: StationsController.cs
 * Description: Station API endpoints
 * Author: Gabilan (Station Management)
 * Date: 21/09/2026
 */
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class StationsController : ControllerBase
{
    private readonly IStationService _stationService;

    // Inject station service
    public StationsController(IStationService stationService)
    {
        _stationService = stationService;
    }

    // POST create station
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateStationDto request)
    {
        var result = await _stationService.CreateStationAsync(request);
        return Ok(result);
    }

    // GET all stations
    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var stations = await _stationService.GetAllStationsAsync();
        return Ok(stations);
    }

    // PUT update station
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateStationDto request)
    {
        var result = await _stationService.UpdateStationAsync(id, request);
        if (result.Success) return Ok(result);
        return NotFound(result);
    }

    // PUT deactivate station
    [HttpPut("{id}/deactivate")]
    public async Task<IActionResult> Deactivate(string id)
    {
        var result = await _stationService.DeactivateStationAsync(id);
        if (result.Success) return Ok(result);
        return BadRequest(result);
    }

    // GET nearby stations by lat/lng
    [HttpGet("nearby")]
    public async Task<IActionResult> GetNearby([FromQuery] double lat, [FromQuery] double lng, [FromQuery] double radiusKm = 10)
    {
        var stations = await _stationService.GetNearbyStationsAsync(lat, lng, radiusKm);
        return Ok(stations);
    }
}
