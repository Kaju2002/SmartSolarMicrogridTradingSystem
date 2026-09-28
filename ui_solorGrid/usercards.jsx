function UserCard({ user }) {
    return (
      <div className="user-card">
        <div className="avatar">
          {user.name.charAt(0)}
        </div>
  
        <div className="user-info">
          <h3>{user.name}</h3>
          <p>{user.email}</p>
          <span>{user.role}</span>
        </div>
  
        <button onClick={() => alert(`Selected ${user.name}`)}>
          View
        </button>
      </div>
    );
  }
  
  export default UserCard;