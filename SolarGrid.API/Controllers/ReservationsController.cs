using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class ReservationsController : ControllerBase
{
    private readonly IReservationService _reservationService;

    public ReservationsController(IReservationService reservationService)
    {
        _reservationService = reservationService;
    }

    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateReservationDto request)
    {
        var result = await _reservationService.CreateReservationAsync(request);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationDto request)
    {
        var result = await _reservationService.UpdateReservationAsync(id, request);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> Cancel(string id)
    {
        var result = await _reservationService.CancelReservationAsync(id);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var list = await _reservationService.GetAllReservationsAsync();
        return Ok(list);
    }

    [HttpGet("prosumer/{nic}")]
    public async Task<IActionResult> GetByProsumer(string nic)
    {
        var list = await _reservationService.GetReservationsByProsumerAsync(nic);
        return Ok(list);
    }

    [HttpPut("{id}/approve")]
    public async Task<IActionResult> Approve(string id, [FromQuery] string approvedByUserId)
    {
        var result = await _reservationService.ApproveReservationAsync(id, approvedByUserId);

        if (result.Success)
            return Ok(result);

        return NotFound(result);
    }
}
