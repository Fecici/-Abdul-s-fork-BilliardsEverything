-- The Markdown source keeps its own H1 so it remains readable without Pandoc.
-- The standalone edition gets a metadata title, so remove that duplicate H1
-- and promote the remaining hierarchy for a useful HTML/PDF document outline.
local expected_title = "Abdul Windows BilliardsEverything: Codebase Handbook"
local removed_source_title = false

function RawInline(raw)
  if raw.format == "tex" then
    error("TeX outside math delimiters would disappear from HTML: " .. raw.text)
  end
end

function RawBlock(raw)
  if raw.format == "tex" then
    error("TeX block outside supported math delimiters would disappear from HTML")
  end
end

function Header(header)
  if not removed_source_title then
    local actual_title = pandoc.utils.stringify(header.content)
    if header.level ~= 1 or actual_title ~= expected_title then
      error("unexpected first handbook header: " .. actual_title)
    end

    removed_source_title = true
    return {}
  end

  header.level = math.max(1, header.level - 1)
  return header
end
