using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IAuthService
{
    Task<LoginResponseDto> LoginAsync(LoginRequestDto request);
    Task<LoginResponseDto> RegisterAsync(RegisterRequestDto request);
    Task<LoginResponseDto> UpdateUserStatusAsync(UpdateUserStatusDto request);
    Task<List<User>> GetPendingUsersAsync();
    Task<User?> GetProfileAsync(string userId);
    Task<LoginResponseDto> UpdateProfileAsync(string userId, UpdateProfileDto request);
}
