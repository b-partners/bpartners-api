<!doctype html>
<html class="no-js" lang="fr">
<head>
    <meta charset="utf-8">
    <meta name="description" content="">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Invoice</title>
    <link rel="stylesheet" href="./style/style.css">
</head>
<body>
<header>
    <div class="flex-container">
        <div class="flex-content">
            <img class="logo" src="./images/logo.png" alt="logo">
            <div>
                <h1>BPartners</h1>
                <p>SIREN : XXXXXXXX</p>
            </div>
        </div>
        <div class="flex-content">
            <img class="code" src="./images/code.png" alt="QR-code">
            <div>
                <a href="">Payez ici</a>
                <p>Nom client : John Doe</p>
                <p>Addresse : Rue de Liege</p>
                <p>Telephone : +33 6 14 454 45</p>
                <p>Email : example@email.com</p>
                <p>Website : www.website.fr</p>
                <p>IBAN : 015488</p>
            </div>
        </div>
    </div>
</header>
<section>
    <p>N Facture : REF XXXXX</p>
    <p>Date : 03/10/2022</p>
    <table border>
        <thead>
        <th>Description</th>
        <th>Quantite</th>
        <th>Prix unitaire</th>
        <th>Remise</th>
        <th>Montant HT</th>
        <th>Montant TTC</th>
        </thead>
        <tbody>
        <tr>
            <td>Example of description</td>
            <td>50</td>
            <td>150</td>
            <td>5%</td>
            <td>7500</td>
            <td>8000</td>
        </tr>
        <tr>
            <td>Example of description</td>
            <td>50</td>
            <td>150</td>
            <td>5%</td>
            <td>7500</td>
            <td>8000</td>
        </tr>
        </tbody>
    </table>
    <div class="amount-section">
        <div>
            <p><strong>Total HT</strong> : 7500 </p>
            <p><strong>Montant de TVA</strong> : 20%</p>
            <p><strong>Total TTC</strong> : 8000</p>
        </div>
    </div>
</section>
<footer>
    <p>Lorem ip sum dolor</p>
    <hr/>
    <p style="text-align: center">BPartners SASU - Capital XXXXXXXXX</p>
    <div class="footer-info">
        <div>
            <p>Siege : Rue XXX PARIS</p>
            <p>Contact : + 33 05 12 456 78</p>
        </div>
        <div>
            <p>Email : example@email.com</p>
            <p>Website : www.website.com</p>
        </div>
    </div>
</footer>
</body>
</html>