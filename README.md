About [MiniGoods]
==================

Main purpose of this software is in quick changing prices of goods on cash register **Unisystem Mini T-500.02ME(Ukraine)**.
Due to changes in tax legislation of Ukraine, owners of cash register can(or must) increase the prices by 5% on some of goods,
located in cash register. To automate routine I write this app.

This application allows to increase goods prices across all trade base by specifying margin multiplier
and ignored numbers of commodity's, which do not require changes.
Cash register have a database in .dbf format.
There are a several utils for changing .dbf files, but they did not satisfy my requirements.

Preparation
------------------------
First of all you need to get Unisystem's software to obtain database file with goods from cash register
by downloading and installing [this](http://img.unisystem.ua/files/6/MINI_prog_v2004_4_02.zip) installer.
Install, ckick, ckick, ckick and then read data from cash device and save it in to few files.
If you do not know how to work with Unisystem's software, it is probably not necessary for you to do this at all.
After all you must have a **C_goods.dbf** file.

Get started
===========
For using app, you must typed in console `java -jar MiniGoods.jar`.
If you run app without any params, it will raise all prices on 5% without excludes and save to `new_C_goods.dbf` file.

Console params
==============
*`name` - different name for `C_goods.dbf` file
*`margin` - set increase margin on this format x.xx, where 0.05 will be a 5%
*`max_good_item` - 4000 by default, change for your maximum goods item count
*`skipped_files` - text file, where you can specify ignored items in database.
    There are a several rules for marking ignored file. You can separate your items by space, by ";" and ",".
    To specify the range of items use "-" char, like `345-433`.

For example, using this tool will look's like:
    `java -jar MiniGoods.jar C_goods.dbf 0.05 1230 skipped.txt`
Where the result will be a new file `new_C_goods.dbf` in same directory, which will consist increased on 5%
prices on goods in range from 1 to 1230, and excluded items, selected in skipped.txt

Afterword
=========
I hope this app will be useful for Ukrainian(where this cash devices are widely using) workers of service center of cash devices.