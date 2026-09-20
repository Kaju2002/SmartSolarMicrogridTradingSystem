using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class AuthService : IAuthService
{
    private readonly IMongoCollection<User> _users;

    public AuthService(MongoDbContext dbContext)
    {
        _users = dbContext.Database.GetCollection<User>("Users");
    }

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

        return new LoginResponseDto
        {
            Success = true,
            Message = "Login successful",
            UserType = user.UserType,
            UserId = user.Id,
            FullName = user.FullName
        };
    }

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
}