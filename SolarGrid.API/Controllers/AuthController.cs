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
}