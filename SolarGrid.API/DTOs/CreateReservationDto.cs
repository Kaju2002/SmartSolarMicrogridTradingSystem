namespace SolarGrid.API.DTOs;

public class CreateReservationDto
{
    public string ProsumerNic { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public DateTime ReservationDateTime { get; set; }
}
