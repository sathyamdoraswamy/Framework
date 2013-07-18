<?php
echo "<html>";
echo "<body>";

session_start();
$timestamp = $_POST["record"];
$groupid = $_SESSION['groupids'][$_SESSION['n']];

if($timestamp!=null) {
$cmd="bash /home/sathyam/Desktop/Interface/a.sh $groupid $timestamp 2>&1";
$error=`$cmd`;
}
//echo $error;
header("Location:home.php");
echo "</body>";
echo "</html>";
?>
