$(document).ready(function() {
    setInterval(getUsersOnline, 500);
});

function getUsersOnline() {
    $.ajax({
        type: "GET",
        url: "/getUsersOnline",
        success: function(data)
        {
            let usersOnline = data["usersOnline"];

            $("#usersOnline").html("");

            let activeUsersCount = 0;

            for (var i = 0; i < usersOnline.length; i++) {
                if ((usersOnline[i]["expirationTime"]["N"] - (new Date()).getTime() / 1000) < 0) {
                    continue;
                }

                let output = '<div class="panel panel-default"> \
                             <div class="panel-body">' +
                             '<b>' + usersOnline[i]["username"]["S"][0] + '</b>' +
                             ' (<i>' + usersOnline[i]["username"]["S"][1] + '</i>)' +
                             ', user\'s state: ' + '<span class="label label-success">' +
                             usersOnline[i]["userState"]["S"] + '</span>' +
                             ', device state: ' + '<span class="label label-primary">' +
                             usersOnline[i]["deviceState"]["S"] + '</span>'
                             '</div> \
                             </div>';

                $("#usersOnline").append(output);

                activeUsersCount++;
            }

            if (activeUsersCount == 0) {
                $("#usersOnline").html("<i>There are no users online at the moment</i>");
            }
        }
    });
}
