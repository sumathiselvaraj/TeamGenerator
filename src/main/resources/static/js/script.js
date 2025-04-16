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
    
    // Show/hide event-specific instructions based on dropdown selection
    const eventTypeSelect = document.getElementById('eventType');
    if (eventTypeSelect) {
        eventTypeSelect.addEventListener('change', function() {
            // Hide all instruction blocks
            document.querySelectorAll('.event-instructions').forEach(function(instructions) {
                instructions.style.display = 'none';
            });
            
            // Show default instructions if no selection
            const defaultInstructions = document.getElementById('default-instructions');
            
            if (this.value) {
                // Get the selected event type and show its instructions
                const selectedInstructionsId = this.value + '-instructions';
                const selectedInstructions = document.getElementById(selectedInstructionsId);
                
                if (selectedInstructions) {
                    selectedInstructions.style.display = 'block';
                    if (defaultInstructions) {
                        defaultInstructions.style.display = 'none';
                    }
                } else {
                    // If no specific instructions found, show default
                    if (defaultInstructions) {
                        defaultInstructions.style.display = 'block';
                    }
                }
            } else {
                // No selection, show default
                if (defaultInstructions) {
                    defaultInstructions.style.display = 'block';
                }
            }
            
            // Update example table based on event type
            updateExampleTable(this.value);
        });
    }
    
    // Function to update example table based on event type
    function updateExampleTable(eventType) {
        const tableExample = document.getElementById('file-format-example');
        
        if (!tableExample) return;
        
        if (eventType === 'SQL_BOOTCAMP') {
            tableExample.innerHTML = `
                <thead>
                    <tr>
                        <th>Timestamp</th>
                        <th>Email Address</th>
                        <th>Full Name</th>
                        <th>Track</th>
                        <th>Batch No</th>
                        <th>Course Type</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>2025-04-01 10:30:15</td>
                        <td>john.doe@example.com</td>
                        <td>John Doe</td>
                        <td>SDET</td>
                        <td>31</td>
                        <td>Full course topics</td>
                    </tr>
                    <tr>
                        <td>2025-04-01 11:15:20</td>
                        <td>jane.smith@example.com</td>
                        <td>Jane Smith</td>
                        <td>DA</td>
                        <td>31</td>
                        <td>only Advanced topics</td>
                    </tr>
                </tbody>`;
        } else if (eventType === 'SELENIUM_HACKATHON') {
            tableExample.innerHTML = `
                <thead>
                    <tr>
                        <th>Timestamp</th>
                        <th>Email Address</th>
                        <th>Full Name</th>
                        <th>Track with Batch No</th>
                        <th>Are you working?</th>
                        <th>Time Zone</th>
                        <th>Completed DSAlgo?</th>
                        <th>Previous hackathon?</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>2025-04-01 10:30:15</td>
                        <td>john.doe@example.com</td>
                        <td>John Doe</td>
                        <td>SDET B31</td>
                        <td>Yes</td>
                        <td>EST</td>
                        <td>Yes</td>
                        <td>No</td>
                    </tr>
                    <tr>
                        <td>2025-04-01 11:15:20</td>
                        <td>jane.smith@example.com</td>
                        <td>Jane Smith</td>
                        <td>DA B31</td>
                        <td>No</td>
                        <td>PST</td>
                        <td>Yes</td>
                        <td>Yes</td>
                    </tr>
                </tbody>`;
        } else {
            // Default example
            tableExample.innerHTML = `
                <thead>
                    <tr>
                        <th>Timestamp</th>
                        <th>Email Address</th>
                        <th>Full Name</th>
                        <th>Track</th>
                        <th>Batch No</th>
                        <th>Course Type</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>2025-04-01 10:30:15</td>
                        <td>john.doe@example.com</td>
                        <td>John Doe</td>
                        <td>SDET</td>
                        <td>31</td>
                        <td>Full course topics</td>
                    </tr>
                    <tr>
                        <td>2025-04-01 11:15:20</td>
                        <td>jane.smith@example.com</td>
                        <td>Jane Smith</td>
                        <td>DA</td>
                        <td>31</td>
                        <td>only Advanced topics</td>
                    </tr>
                </tbody>`;
        }
    }
    
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
