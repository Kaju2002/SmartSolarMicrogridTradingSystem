using MongoDB.Bson;
using SolarGrid.API.Data;
using MongoDB.Driver;
var builder = WebApplication.CreateBuilder(args);

builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// MongoDB DI register
builder.Services.AddSingleton<MongoDbContext>();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// Temporary: MongoDB ping test
app.MapGet("/ping-db", async (MongoDbContext db) =>
{
    await db.Database.RunCommandAsync((Command<BsonDocument>)"{ping:1}");
    return Results.Ok(new { status = "MongoDB connected" });
});

app.Run();