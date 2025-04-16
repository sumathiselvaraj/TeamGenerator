document.addEventListener('DOMContentLoaded', function() {
    // Initialize Feather icons
    feather.replace();
    
    // Toggle team details visibility
    document.querySelectorAll('.toggle-team-details').forEach(function(toggle) {
        toggle.addEventListener('click', function() {
            const teamCard = this.closest('.team-card');
            const teamMembers = teamCard.querySelector('.team-members');
            
            if (teamMembers.style.display === 'none') {
                teamMembers.style.display = 'block';
                this.style.transform = 'rotate(180deg)';
            } else {
                teamMembers.style.display = 'none';
                this.style.transform = 'rotate(0deg)';
            }
        });
    });
    
    // File input change handler to show selected filename
    const fileInput = document.getElementById('file');
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            const fileName = this.files[0]?.name || 'No file chosen';
            const fileLabel = this.nextElementSibling;
            if (fileLabel && fileLabel.classList.contains('input-group-text')) {
                fileLabel.textContent = fileName.length > 20 
                    ? fileName.substring(0, 17) + '...' 
                    : fileName;
            }
        });
    }
    
    // Add event listener for tab navigation
    const triggerTabList = [].slice.call(document.querySelectorAll('#teamsTab button'));
    triggerTabList.forEach(function (triggerEl) {
        const tabTrigger = new bootstrap.Tab(triggerEl);
        
        triggerEl.addEventListener('click', function (event) {
            event.preventDefault();
            tabTrigger.show();
        });
    });
});
