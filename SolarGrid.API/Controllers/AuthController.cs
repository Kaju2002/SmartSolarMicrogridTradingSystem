/*
 * File: AuthController.cs
 * Description: Auth API endpoints
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class AuthController : ControllerBase
{
    private readonly IAuthService _authService;

    // Inject auth service
    public AuthController(IAuthService authService)
    {
        _authService = authService;
    }

    // POST login
    [AllowAnonymous]
    [HttpPost("login")]
    public async Task<IActionResult> Login([FromBody] LoginRequestDto request)
    {
        var result = await _authService.LoginAsync(request);

        if (result.Success)
            return Ok(result);

        return Unauthorized(result);
    }

    // POST register — public Prosumer only
    [AllowAnonymous]
    [HttpPost("register")]
    public async Task<IActionResult> Register([FromBody] RegisterRequestDto request)
    {
        var result = await _authService.RegisterAsync(request);

        if (result.Success)
            return Ok(result);

        return Conflict(result);
    }

    // POST create Grid Operator — Backoffice only
    [Authorize(Roles = "Backoffice")]
    [HttpPost("operators")]
    public async Task<IActionResult> CreateOperator([FromBody] RegisterRequestDto request)
    {
        var result = await _authService.CreateGridOperatorAsync(request);

        if (result.Success)
            return Ok(result);

        return Conflict(result);
    }

    // PUT update user status — Backoffice only
    [Authorize(Roles = "Backoffice")]
    [HttpPut("update-status")]
    public async Task<IActionResult> UpdateStatus([FromBody] UpdateUserStatusDto request)
    {
        var result = await _authService.UpdateUserStatusAsync(request);

        if (result.Success)
            return Ok(result);

        return NotFound(result);
    }

    // GET pending approval users — Backoffice only
    [Authorize(Roles = "Backoffice")]
    [HttpGet("pending-users")]
    public async Task<IActionResult> GetPendingUsers()
    {
        var users = await _authService.GetPendingUsersAsync();
        return Ok(users);
    }

    // GET Grid Operators and Prosumers — Backoffice only
    [Authorize(Roles = "Backoffice")]
    [HttpGet("users")]
    public async Task<IActionResult> GetUsers([FromQuery] string? userType = null)
    {
        var users = await _authService.GetUsersAsync(userType);
        return Ok(users);
    }

    // GET profile by user id — self or Backoffice
    [HttpGet("profile/{userId}")]
    public async Task<IActionResult> GetProfile(string userId)
    {
        if (!CanAccessProfile(userId))
            return Forbid();

        var user = await _authService.GetProfileAsync(userId);
        if (user is null)
            return NotFound();

        return Ok(user);
    }

    // PUT update profile fields — self or Backoffice
    [HttpPut("profile/{userId}")]
    public async Task<IActionResult> UpdateProfile(string userId, [FromBody] UpdateProfileDto request)
    {
        if (!CanAccessProfile(userId))
            return Forbid();

        var result = await _authService.UpdateProfileAsync(userId, request);

        if (!result.Success)
            return NotFound(result);

        return Ok(result);
    }

    // PUT request account deactivation — self or Backoffice
    [HttpPut("request-deactivation/{userId}")]
    public async Task<IActionResult> RequestDeactivation(string userId)
    {
        if (!CanAccessProfile(userId))
            return Forbid();

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

    private bool CanAccessProfile(string userId)
    {
        if (User.IsBackoffice())
            return true;

        var currentId = User.GetUserId();
        return !string.IsNullOrEmpty(currentId)
            && string.Equals(currentId, userId, StringComparison.Ordinal);
    }
}
