local args = { ... }

if ccemux then
    local function help()
        print("Example usages:")
        print("emu close - close this emulator")
        print("emu open - open an emulator with the next ID")
        print("emu open 2 - open an emulator with ID 2")
        print("emu data - opens the data folder")
        print("emu set <setting> <values> - edits a setting")
        print("emu list settings - list editable settings")
    end

    if #args == 0 then
        help()
    else
        if args[1] == "close" then
            ccemux.closeEmu()
        elseif args[1] == "open" then
            print("Opened computer ID " .. ccemux.openEmu(tonumber(args[2])))
        elseif args[1] == "data" then
            if ccemux.openDataDir() then
                print("Opened data folder")
            else
                print("Unable to open data folder")
            end
        elseif args[1] == "set" then
            if #args <= 1 then
                help()
            else
                if args[2] == "resolution" then
                    if #args == 4 then
                        ccemux.setResolution(tonumber(args[3]), tonumber(args[4]))
                        print("Set resolution to " .. args[3] .. "x" .. args[4])
                    elseif #args == 3 then
                        if args[3] == "computer" then
                            ccemux.setResolution(51, 19)
                            print("Set resolution to computer (51x19)")
                        elseif args[3] == "pocket" then
                            ccemux.setResolution(26, 20)
                            print("Set resolution to pocket (26x20)")
                        end
                    else
                        printError("Usage: emu set resolution <width> <height>")
                    end
                elseif args[2] == "scale" then

                elseif args[2] == "cursor" then
                    ccemux.setCursorChar(args[3])
                    print("Set cursor char to " .. args[3])
                else
                    printError("Unrecognized setting: " .. args[2])
                end
            end
        elseif args[1] == "list" then
            if args[2] == "settings" then
                print("Editable settings:")
                print("resolution <width> <height>")
                print("scale <pixels>")
                print("cursor <char>")
            else
                printError("Unrecognized subcommand: " .. args[2])
            end
        else
            printError("Unrecognized subcommand: " .. args[1])
            help()
        end
    end
else
    printError("CCEmuX API is disabled or unavailable.")
end