plotsPlugin
===========

Plots plugin developed for the Arcane Survival server.

Author: "Morios" Mark Talrey

Current Version: RC.3.0 for Minecraft 1.7.10

Notes:
- Plot data is saved to "plots.ser" in the main server folder. This is serialized object data created by Java, and may not be human-readable.
- Currently saves on reload or server stop. I may add timed saves in at a later date.
- Credits integration is halfway there! It works, but everyone has carte blanche right now, lol.

Commands:
- credits: See your balance or send credits to another player (/credits give <name> #)
- plot: master command for plots. Defaults to 'list' option.
- plot buy: creates a plot. Can create using corners or a radius. Defaults to radius.
- plot sell: Removes a plot.
- plot modify: Add or remove a user from the owner list. Use + or - and the name.
- plot list: Lists who is an owner of this plot. You don't need to own it to use this.
- plot edit: Enables or disables the plot's safeguard. Use with caution. Defaults to toggle.
- plot confirm (alias: confirm): Confirm creation or destruction of a plot.
- plot cancel (alias: cancel): Cancel creation or destruction of a plot.
