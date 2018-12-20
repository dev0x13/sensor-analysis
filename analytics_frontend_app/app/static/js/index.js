$(document).ready(function() {
  setInterval(getUsersOnline, 500);
});

function getUsersOnline() {
  $.ajax({
          type: "GET",
          url: "/getUsersOnline",
          success: function(data)
          {
            let output = "";
            let usersOnline = data["usersOnline"];

            for (var i = 0; i < usersOnline.length; i++) {
              output += '<div class="panel panel-default"> \
                <div class="panel-body">' +
                  '<b>' + usersOnline[i]["username"]["S"][0] + '</b>' +
                  ' (<i>' + usersOnline[i]["username"]["S"][1] + '</i>)' +
                  ', user\'s state: ' + '<span class="label label-success">' +
                  usersOnline[i]["userState"]["S"] + '</span>' +
                  ', device state: ' + '<span class="label label-primary">' +
                  usersOnline[i]["deviceState"]["S"] + '</span>'
                '</div> \
              </div>';
            }

            $("#usersOnline").html(output);
          }
      });
}
