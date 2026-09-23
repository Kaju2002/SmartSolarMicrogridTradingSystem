/*
 * File: IAuthService.cs
 * Description: Auth service contract
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public interface IAuthService
{
    // Login
    Task<LoginResponseDto> LoginAsync(LoginRequestDto request);
    // Register
    Task<LoginResponseDto> RegisterAsync(RegisterRequestDto request);
    // Update status
    Task<LoginResponseDto> UpdateUserStatusAsync(UpdateUserStatusDto request);
    // Pending users
    Task<List<User>> GetPendingUsersAsync();
    // List Grid Operators and Prosumers (optional role filter)
    Task<List<User>> GetUsersAsync(string? userType = null);
    // Get profile
    Task<User?> GetProfileAsync(string userId);
    // Update profile
    Task<LoginResponseDto> UpdateProfileAsync(string userId, UpdateProfileDto request);
}
