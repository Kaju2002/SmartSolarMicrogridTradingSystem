/*
 * File: AuthService.cs
 * Description: Login, register, profile and user status
 * Author: Vithusha (Identity and Access)
 * Date: 20/09/2026
 */
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class AuthService : IAuthService
{
    private readonly IMongoCollection<User> _users;
    private readonly JwtTokenHelper _jwtTokenHelper;

    // Setup Users collection and JWT helper
    public AuthService(MongoDbContext dbContext, JwtTokenHelper jwtTokenHelper)
    {
        _users = dbContext.Database.GetCollection<User>("Users");
        _jwtTokenHelper = jwtTokenHelper;
    }

    // Login with NIC or username and check password
    public async Task<LoginResponseDto> LoginAsync(LoginRequestDto request)
    {
        var filter = Builders<User>.Filter.Or(
            Builders<User>.Filter.Eq(u => u.Nic, request.Identifier),
            Builders<User>.Filter.Eq(u => u.Username, request.Identifier)
        );

        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Invalid credentials"
            };
        }

        var passwordValid = BCrypt.Net.BCrypt.Verify(request.Password, user.PasswordHash);

        if (!passwordValid)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "Invalid credentials"
            };
        }

        if (user.Status != "Active")
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = $"Account is {user.Status}. Please contact Backoffice."
            };
        }

        var token = _jwtTokenHelper.GenerateToken(user);

        return new LoginResponseDto
        {
            Success = true,
            Message = "Login successful",
            UserType = user.UserType,
            UserId = user.Id,
            FullName = user.FullName,
            Token = token
        };
    }

    // Create new user with hashed password
    public async Task<LoginResponseDto> RegisterAsync(RegisterRequestDto request)
    {
        var filters = new List<FilterDefinition<User>>();

        if (!string.IsNullOrWhiteSpace(request.Username))
            filters.Add(Builders<User>.Filter.Eq(u => u.Username, request.Username));

        if (!string.IsNullOrWhiteSpace(request.Nic))
            filters.Add(Builders<User>.Filter.Eq(u => u.Nic, request.Nic));

        if (filters.Count > 0)
        {
            var existing = await _users.Find(Builders<User>.Filter.Or(filters)).FirstOrDefaultAsync();
            if (existing is not null)
            {
                return new LoginResponseDto
                {
                    Success = false,
                    Message = "Username or NIC already exists"
                };
            }
        }

        var status = string.Equals(request.UserType, "Prosumer", StringComparison.OrdinalIgnoreCase)
            ? "PendingApproval"
            : "Active";

        var newUser = new User
        {
            UserType = request.UserType,
            Nic = request.Nic,
            Username = request.Username,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(request.Password),
            FullName = request.FullName,
            Email = request.Email,
            PhoneNumber = request.PhoneNumber,
            Status = status,
            CreatedAt = DateTime.UtcNow
        };

        await _users.InsertOneAsync(newUser);

        return new LoginResponseDto
        {
            Success = true,
            Message = "User registered",
            UserId = newUser.Id,
            UserType = newUser.UserType,
            FullName = newUser.FullName
        };
    }

    // Change account status (approve / deactivate / reactivate)
    public async Task<LoginResponseDto> UpdateUserStatusAsync(UpdateUserStatusDto request)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, request.UserId);
        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "User not found"
            };
        }

        var update = Builders<User>.Update
            .Set(u => u.Status, request.NewStatus)
            .Set(u => u.UpdatedAt, DateTime.UtcNow);

        await _users.UpdateOneAsync(filter, update);

        return new LoginResponseDto
        {
            Success = true,
            Message = $"Status updated to {request.NewStatus}",
            UserId = user.Id,
            UserType = user.UserType,
            FullName = user.FullName
        };
    }

    // List users waiting for Backoffice approval
    public async Task<List<User>> GetPendingUsersAsync()
    {
        var filter = Builders<User>.Filter.Eq(u => u.Status, "PendingApproval");
        return await _users.Find(filter).ToListAsync();
    }

    // Get one user profile by id
    public async Task<User?> GetProfileAsync(string userId)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, userId);
        return await _users.Find(filter).FirstOrDefaultAsync();
    }

    // Update name, email and phone only
    public async Task<LoginResponseDto> UpdateProfileAsync(string userId, UpdateProfileDto request)
    {
        var filter = Builders<User>.Filter.Eq(u => u.Id, userId);
        var user = await _users.Find(filter).FirstOrDefaultAsync();

        if (user is null)
        {
            return new LoginResponseDto
            {
                Success = false,
                Message = "User not found"
            };
        }

        var update = Builders<User>.Update
            .Set(u => u.FullName, request.FullName)
            .Set(u => u.Email, request.Email)
            .Set(u => u.PhoneNumber, request.PhoneNumber)
            .Set(u => u.UpdatedAt, DateTime.UtcNow);

        await _users.UpdateOneAsync(filter, update);

        return new LoginResponseDto
        {
            Success = true,
            Message = "Profile updated",
            UserId = user.Id,
            UserType = user.UserType,
            FullName = request.FullName
        };
    }
}
