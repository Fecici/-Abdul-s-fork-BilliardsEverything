#pragma once

#include "code_sequence.hpp"
#include "general.hpp"

#include <boost/thread.hpp>
#include <boost/optional.hpp>
#include <mutex>
#include <thread>

class RationalLineSegment final {

  public:
    PointQ point0;
    LinComArrZ<XYEta> line0;
    PointQ point1;
    LinComArrZ<XYEta> line1;

    explicit RationalLineSegment(const PointQ& point0_,
                                 const LinComArrZ<XYEta>& line0_,
                                 const PointQ& point1_,
                                 const LinComArrZ<XYEta>& line1_)
        : point0{point0_},
          line0{line0_},
          point1{point1_},
          line1{line1_} {
    }
};

boost::optional<RationalLineSegment> calculate_bounding_line_segment(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles, const LinComArrZ<XYEta>& constraint);

class RationalPair final {
  public:
    PointQ point;
    LinComArrZ<XYEta> side_line;

    explicit RationalPair(const PointQ& point_, const LinComArrZ<XYEta>& side_line_)
        : point{point_},
          side_line{side_line_} {
    }
};

// PointQ is the point, and LinComArrZ<XYEta> is the line that goes from that
// point to the next one
using RationalPolygon = std::vector<RationalPair>;

boost::optional<RationalPolygon> calculate_bounding_polygon(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles);
