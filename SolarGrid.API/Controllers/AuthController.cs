using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequestDto request)
    {
        var result = await _authService.LoginAsync(request);

        if (result.Success)
            return Ok(result);

        return Unauthorized(result);
    }

    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequestDto request)
    {
        var result = await _authService.RegisterAsync(request);

        if (result.Success)
            return Ok(result);

        return Conflict(result);
    }

    [HttpPut("update-status")]
    public async Task<IActionResult> UpdateStatus([FromBody] UpdateUserStatusDto request)
    {
        var result = await _authService.UpdateUserStatusAsync(request);

        if (result.Success)
            return Ok(result);

        return NotFound(result);
    }

    [HttpGet("pending-users")]
    public async Task<IActionResult> GetPendingUsers()
    {
        var users = await _authService.GetPendingUsersAsync();
        return Ok(users);
    }

    [HttpGet("profile/{userId}")]
    public async Task<IActionResult> GetProfile(string userId)
    {
        var user = await _authService.GetProfileAsync(userId);
        if (user is null)
            return NotFound();

        return Ok(user);
    }

    [HttpPut("profile/{userId}")]
    public async Task<IActionResult> UpdateProfile(string userId, [FromBody] UpdateProfileDto request)
    {
        var result = await _authService.UpdateProfileAsync(userId, request);

        if (!result.Success)
            return NotFound(result);

        return Ok(result);
    }

    [HttpPut("request-deactivation/{userId}")]
    public async Task<IActionResult> RequestDeactivation(string userId)
    {
        var request = new UpdateUserStatusDto
        {
            UserId = userId,
            NewStatus = "Deactivated"
        };

        var result = await _authService.UpdateUserStatusAsync(request);

        if (!result.Success)
            return NotFound(result);

        return Ok(result);
    }
}