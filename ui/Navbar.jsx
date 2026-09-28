function Navbar() {
    return (
      <nav className="navbar">
        <div className="logo">SampleApp</div>
  
        <div className="nav-links">
          <a href="/">Home</a>
          <a href="/users">Users</a>
          <a href="/about">About</a>
        </div>
  
        <button className="profile-btn">
          Profile
        </button>
      </nav>
    );
  }
  
  export default Navbar;