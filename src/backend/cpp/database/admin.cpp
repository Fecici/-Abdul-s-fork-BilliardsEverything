#include <array>
#include <boost/format.hpp>

#include "database/admin.hpp"
#include "sqlite.hpp"

void database::create(const std::string& db_path) {

    constexpr auto flags = sqlite::Open::Readwrite | sqlite::Open::Create;

    sqlite::Database db{db_path, flags};

    // Things are faster (not surprisingly) when you have multiple smaller
    // tables rather than one giant one

    // Each point is an x y pair, so x-space-y
    // left_right is left_number left_branch right_number right_branch
    // initial angles would be like x y (in lowercase). The way they are presented
    // can be different though

    // We have one unified table for everything. Stuff that is easy to calculate
    // (code length, code sum, line_coeffs, etc.) are left out. Only the computationally
    // expensive MRR stuff is left in. Creating a table for each type is handy though,
    // because that splits things apart a bit, and should make it a little more speedy

    const std::string schema = "create table %1%("
                               "code_sequence text check(typeof(code_sequence) = 'text'),"

                               "initial_angles text check(typeof(initial_angles) = 'text'),"

                               "points text check(typeof(points) = 'text'),"
                               // In this case, we can't separate the equations into sines and
                               // cosines, because we need to know the specific order of the equations
                               // and how they relate to
                               "equations text check(typeof(equations) = 'text'),"

                               // If we wanted, we could optimize the database even further
                               // by potentially using null for these and foreign keys
                               // Unless it is possible that an infinite pattern changes
                               // the code type. Then we can't do that. I think this is
                               // ok for now.
                               "left_rights text check(typeof(left_rights) = 'text'),"
                               // We use an empty array (in this case empty string)
                               // as a sentinel value for one we calculated from scratch
                               // We could use a foreign key here, and then null for values
                               // that don't have a lr code sequence.
                               "lr_code_sequence text check(typeof(lr_code_sequence) = 'text'),"

                               "polygon text check(typeof(polygon) = 'text'),"
                               "sin_equations text check(typeof(sin_equations) = 'text'),"
                               "cos_equations text check(typeof(cos_equations) = 'text'),"
                               "primary key (code_sequence)"
                               ");";

    const std::array<std::string, 5> tables{{"oso", "osno", "cs", "cns", "ons"}};

    for (const auto& table : tables) {
        const std::string sql = str(boost::format(schema) % table);
        db.prepare(sql).bind().exec();
    }
}

void database::clear(const std::string& db_path) {

    constexpr auto flags = sqlite::Open::Readwrite;

    sqlite::Database db{db_path, flags};

    const std::array<std::string, 5> tables{{"oso", "osno", "cs", "cns", "ons"}};

    for (const auto& table : tables) {
        const std::string sql = str(boost::format("delete from %1%;") % table);
        db.prepare(sql).bind().exec();
    }
}
