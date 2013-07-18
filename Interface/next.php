<?php
session_start();
$_SESSION['n'] = $_SESSION['n'] + 1;
header("Location:page.php");
?>
