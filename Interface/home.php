<?php
session_start();
$db = new SQLite3('/home/sathyam/Desktop/Interface/frameworkdb');//fill in the path to the framework database here
$results0 = $db->query('SELECT DISTINCT groupid FROM serverbackup');
$groupids = array();
$_SESSION['l']=0;
while ($row0 = $results0->fetchArray()) {
$groupids[] = $row0['groupid'];
$_SESSION['l']=$_SESSION['l']+1;
}
$_SESSION['groupids']= $groupids;
$_SESSION['n']=0;
header("Location:page.php");
?>
