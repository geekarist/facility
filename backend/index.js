const express = require('express');
const https = require('https');
const app = express();

app.get('/code-ack', (req, res) => {
    const code = req.query.code;
    const tunnel = req.query.tunnel;

    // Construct the URL of the actual endpoint dynamically using the values of 'code' and 'tunnel'
    const actualEndpointUrl = `https://${tunnel}/code-ack?code=${code}`;
    console.log(`Actual endpoint URL: ${actualEndpointUrl}`);

    // Make an HTTP GET request to the actual endpoint using the 'axios' library
    const axios = require('axios');
    axios.get(actualEndpointUrl)
        .then(response => {
            res.send(response.data);
        })
        .catch(error => {
            res.status(500).send(`Error getting ${actualEndpointUrl}: ${error}`);
        });
});

const httpsServer = https.createServer({}, app);

const port = process.env.PORT || 3000;
httpsServer.listen(port, () => {
    console.log(`Wrapper endpoint listening on port ${port}`);
});
