using SolarGrid.API.DTOs;

namespace SolarGrid.API.Services;

public interface IAuthService
{
    Task<LoginResponseDto> LoginAsync(LoginRequestDto request);
    Task<LoginResponseDto> RegisterAsync(RegisterRequestDto request);
    Task<LoginResponseDto> UpdateUserStatusAsync(UpdateUserStatusDto request);
}