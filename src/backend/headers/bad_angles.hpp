#pragma once

#include "general.hpp"

// we don't have to worry about "modding out" any multiples of pi. Any ones that
// show up are simply ones from doing the c = pi - a - b substitution, and those
// are ones we want. Hmmm, it's simpler than I thought.

// Consider the code sequence
// 1 1 2 2 1 1 3 3. It has the code angles
// X Y Z Y Z X Y Z
//
// Side sequence    YZXYXZXYZXZXYX
// Code Angles      XYZZYYZXYYYZZZ
//                  --- - ---  -

// Calculating the path angles is very straight forward. Now, because the number
// of path angles is linear in the code sequence sum, we don't want to calculate
// all of them. Rather, we want to calculate several key angles (the ones underlined)
// and then use those to calculate the rest on demand.

// You calculate these path angles, and then by applying inequalities to them you get
// this bounding region. Very nice.

// TODO write tests for all this

// Suppose you have a code angle, call it X. What are the two sides that correspond to it?
// Well, it could either be YZ or ZY, but which one? On its own, we don't know, but we can
// include an extra piece of information that will tell us. We call the pairs XY, YZ, ZX
// increasing, and the pairs YX, ZY, XZ decreasing (like wrapping alphabetical order). So
// by including whether X is increasing or decreasing, we know which pair it came from.

// Let's say we have a sequence of code angles XYZZYY. Then the corresponding side sequence
// is determined. It is YZXYXZX. So given a bunch of angles (X 1) (Y 1) (Z 2) (Y 2), you
// can determine what the corresponding code sequence is.

// The LR side is also determined by where we are in the sequence, I think.
// So, we go through and find all pairs that repeat. So, all XY pairs in the code sequence

// Each code angle gets a left or right designation, that is the same for all the code angles
// in a code number. So if it is left, and crosses, and then if it is right and crosses.

// Length 1

// Length 2
// So we have XY, YX, ZX, XZ, YZ, ZY
// Then we have map to first index in the code_angles of the pair.
// So, we have a bunch of pairs (A n) (B m)
// Take all pairs, (A n1) (B m1), (A n2) (B m2)
// and look at (A min(n1, n2)) (B min(m1, m2)), since the matching has to be maximal in
// order for the end two to be different. So like
// ZXXXYYX
// ZYXXYYYX
//   ----
// This gives us two pairs: X|XXYY|X and Y|XXYY|Y. These are the real deal
// This gives use two pairs of things to work with. Now we check some stuff:

// Check that the two outside angles are different between the pairs. So in this case,
// X /= Y and X /= Y. This ensures maximality. If one of the sides were equal, we could extend
// both to make them longer. This is ensured by taking the min of the counts on both ends.
// The fact they are different is important, because this means they have a potential of crossing

// Find the LR of the outside start and end for each. Check that they are different (so they
// cross). Then find the left one, and the right one, and then we find the two angles and add
// those as inequalities

// We take the first pair, and check that, based on the length, that the directions of the ones
// at the end are opposite or the same (so -> -> or -> <-) to ensure it crosses. Repeat with
// the second one.

// XYZZXXZXYXXZZZ
// 012 3 456  7
// 00010100012012
// Each code angle is identified by two numbers: the index of the angle within the code_angles vector
// and its number within the code number (starting at 0, of course)

// Length >= 3
// As above, but the code numbers of the ones in the middle must be the same

class CodeInfo final {
  public:
    CodeNumber number;
    XYZ angle;
    bool left;
    LinComArrZ<XYEtaPhi> path_angle;

    explicit CodeInfo(const CodeNumber number_, const XYZ angle_, const bool left_, const LinComArrZ<XYEtaPhi>& path_angle_)
        : number{number_},
          angle{angle_},
          left{left_},
          path_angle{path_angle_} {
    }

    friend std::ostream& operator<<(std::ostream& os, const CodeInfo& info) {
        return os << '(' << info.number << ", " << info.angle << ')'; // << ", " << info.left << ", " << info.path_angle.build() << ')';
    }
};

class CodeInfoVec final {
  public:
    XYZ prev;
    std::vector<CodeInfo> same;
    XYZ next;

    explicit CodeInfoVec(const XYZ prev_, const std::vector<CodeInfo>& same_, const XYZ next_)
        : prev{prev_},
          same{same_},
          next{next_} {
    }

    friend std::ostream& operator<<(std::ostream& os, const CodeInfoVec& info) {
        return os << info.prev << ", " << info.same << ", " << info.next;
    }
};

std::vector<LinComArrZ<XYEtaPhi>> calculate_key_path_angles(const std::vector<std::pair<CodeNumber, XYZ>>& code_nums_angles) {

    std::vector<LinComArrZ<XYEtaPhi>> key_path_angles;

    // This first one is phi (the shooting angle). We don't know the shooting
    // angle in the general case, so we have to start with it as a symbol.
    // (Though for bad angles it doesn't really matter)
    key_path_angles.emplace_back(0, 0, 0, 1);

    // We define the shooting angle so it is on the opposite side of the first
    // code angle.
    bool same_side = false;
    // We don't iterate to the last element because that would give us
    // the key angle for the shooting angle again, but simply expressed
    // differently.
    for (const auto i : falgo::range(code_nums_angles.size() - 1)) {

        const auto& p = code_nums_angles.at(i);

        const auto code_number = p.first;
        const auto code_angle = p.second;

        // Make a copy. Very important, since we are going
        // to mutate it
        auto prev_key_angle = key_path_angles.at(i);

        // same side = add, opposite side = subtract
        if (same_side) {
            prev_key_angle.add(code_number, to_xyetaphi(code_angle));
        } else {
            prev_key_angle.sub(code_number, to_xyetaphi(code_angle));
        }

        key_path_angles.push_back(prev_key_angle);

        same_side = !same_side;
    }

    return key_path_angles;
}

std::vector<CodeInfo> calculate_code_info(const std::vector<std::pair<CodeNumber, XYZ>>& code_nums_angles) {

    const auto path_angles = calculate_key_path_angles(code_nums_angles);

    std::vector<CodeInfo> result;
    for (const auto i : falgo::range(code_nums_angles.size())) {
        // By convention, we start on the left side, and then it alternates
        // for each code number, so the even indices are left, and the odds
        // are right
        const auto left = i % 2 == 0;
        result.emplace_back(code_nums_angles.at(i).first, code_nums_angles.at(i).second, left, path_angles.at(i));
    }

    return result;
}

// We can't decide the left right until we actually have the subunfolding we want. These are
// just a list of candidates to look at.
// Add in the candidates for this code_info
std::map<std::vector<XYZ>, std::vector<CodeInfoVec>> get_candidates(const std::vector<CodeInfo>& code_info) {

    std::map<std::vector<XYZ>, std::vector<CodeInfoVec>> candidates{};

    // Take the list of code angles XYZXYXZYX, and then find all substrings
    // of length >= 2
    // In theory we could also go length 1, but haven't written that up yet
    constexpr size_t min_len = 2;

    // O(n^2)
    const auto size = code_info.size();
    for (const auto i : falgo::range(size)) {
        for (const auto j : falgo::range(i + min_len, size + 1)) {

            // we get the slice [i:j]

            // TODO find some way of making this more functional.
            std::vector<CodeInfo> subvec;
            for (const auto k : falgo::range(i, j)) {
                subvec.push_back(code_info.at(k));
            }

            // Maybe use Boost range in someway, or something?
            std::vector<XYZ> xyz;

            // This is just a map
            for (const auto k : falgo::range(subvec.size())) {
                xyz.push_back(subvec.at(k).angle);
            }

            // prev is the one before i
            // next is the one at j % size
            const auto prev = i == 0 ? falgo::back(code_info).angle : code_info.at(i - 1).angle;
            const auto next = code_info.at(j % size).angle;

            // If the abc isn't in the map already, this constructs the
            // vector for it
            candidates[xyz].emplace_back(prev, subvec, next);
        }
    }

    return candidates;
}

// Check that the code numbers in the range [1:-1] are equal (the inside code numbers)
bool insides_equal(const CodeInfoVec& info_vec0, const CodeInfoVec& info_vec1) {

    for (const auto i : falgo::range(static_cast<size_t>(1), info_vec0.same.size() - 1)) {
        const auto& info0 = info_vec0.same.at(i);
        const auto& info1 = info_vec1.same.at(i);
        if (info0.number != info1.number) {
            return false;
        }
    }

    return true;
}

// 1 2 1 10 1 1 3
// Y Z Y X  Y Z X
bool equal_first(const CodeInfoVec& vec) {

    if (vec.prev != XYZ::Y) {
        return false;
    }

    if (vec.next != XYZ::X) {
        return false;
    }

    const auto& same = vec.same;

    if (same.size() != 5) {
        return false;
    }

    return same.at(0).number == 2 && same.at(1).number == 1 && same.at(2).number == 10 && same.at(3).number == 1 && same.at(4).number == 1 &&
           same.at(0).angle == XYZ::Z && same.at(1).angle == XYZ::Y && same.at(2).angle == XYZ::X &&
           same.at(3).angle == XYZ::Y && same.at(4).angle == XYZ::Z;
}

// 3 1 1 10 1 2 1
// X Z Y X  Y Z Y

// TODO clean this up big time!
// The code sequence must have an even length for this to work, so double odd sequences
std::set<EnumComZ<XYEta>> find_bad_angles(const std::map<std::vector<XYZ>, std::vector<CodeInfoVec>>& candidates) {

    // i, k both zero indexed
    // path angle(angle (i, k)) = key_path_angles.at(i) +/- k*angle

    std::set<EnumComZ<XYEta>> inequalities{};

    for (const auto& kv : candidates) {
        const auto& info_vec_vec = kv.second;

        // We want all combinations of 2 elements, which we can get this way
        for (const auto i : falgo::range(info_vec_vec.size())) {
            const auto& info_vec0 = info_vec_vec.at(i);
            for (const auto j : falgo::range(i + 1, info_vec_vec.size())) {
                const auto& info_vec1 = info_vec_vec.at(j);

                // Check that the inside code numbers are both equal
                if (insides_equal(info_vec0, info_vec1)) {

                    // (A, front_min) ... (B back_min)
                    const auto& front0 = falgo::front(info_vec0.same);
                    const auto& back0 = falgo::back(info_vec0.same);

                    const auto& front1 = falgo::front(info_vec1.same);
                    const auto& back1 = falgo::back(info_vec1.same);

                    // Remember, these need to be maximally repeated subsequences.
                    // If the code numbers of the front or back of the thing are
                    // the same, then we need to make sure if we go backwards
                    // or forwards one, that the corresponding code angles are different

                    // If the code numbers of the fronts of the two are the same,
                    // then the previous code angles must be different
                    if (front0.number == front1.number) {
                        if (info_vec0.prev == info_vec1.prev) {
                            continue;
                        }
                    }

                    // Ditto for the backs
                    if (back0.number == back1.number) {
                        if (info_vec0.next == info_vec1.next) {
                            continue;
                        }
                    }

                    const auto front_min = falgo::min(front0.number, front1.number);
                    const auto back_min = falgo::min(back0.number, back1.number);

                    // start0 | info_vec0 | end0
                    const auto start0_left = front0.number == front_min ? !front0.left : front0.left;
                    const auto start1_left = front1.number == front_min ? !front1.left : front1.left;

                    const auto end0_left = back0.number == back_min ? !back0.left : back0.left;
                    const auto end1_left = back1.number == back_min ? !back1.left : back1.left;

                    // Check that the start and end for each one is different.
                    // This ensures both are LR or RL
                    if (start0_left != end0_left && start1_left != end1_left) {
                        // Now check that they cross. Eg, there is a LR and RL
                        if (start0_left != start1_left) {

                            const auto k0 = front0.number - front_min;
                            const auto k1 = front1.number - front_min;

                            auto angle0 = front0.path_angle;

                            if (front0.left) {
                                angle0.sub(k0, to_abetaphi(front0.angle));
                            } else {
                                angle0.add(k0, to_abetaphi(front0.angle));
                            }

                            auto angle1 = front1.path_angle;

                            if (front1.left) {
                                angle1.sub(k1, to_abetaphi(front1.angle));
                            } else {
                                angle1.add(k1, to_abetaphi(front1.angle));
                            }

                            std::cout << info_vec0 << std::endl;
                            std::cout << info_vec1 << std::endl;

                            LinComArrZ<XYEtaPhi> ineq_phi;
                            if (start0_left) {
                                // 0 is LR, 1 is RL
                                // LR < RL
                                std::cout << angle0.build() << std::endl;
                                std::cout << angle1.build() << std::endl;
                                angle1.sub(angle0);
                                ineq_phi = angle1;
                            } else {
                                // 1 is LR, 0 is RL
                                // LR < RL
                                std::cout << angle1.build() << std::endl;
                                std::cout << angle0.build() << std::endl;
                                angle0.sub(angle1);
                                ineq_phi = angle0;
                            }

                            // Subtracting the angles cancels out the phi
                            auto ineq = remove_phi(ineq_phi);
                            ineq.divide_content();
                            inequalities.insert(ineq.build());

                            std::cout << ineq.build() << std::endl;
                            std::cout << std::endl;
                        }
                    }
                }
            }
        }
    }

    return inequalities;
}

// TODO zip together the code numbers and code angles
// TODO also double them at the beginning, and then pass them into here already doubled
std::set<EnumComZ<XYEta>> calculate_bad_angles(std::vector<std::pair<CodeNumber, XYZ>> code_nums_angles) {

    // Add the candidates for all the rotations and reflections
    std::map<std::vector<XYZ>, std::vector<CodeInfoVec>> candidates;

    const auto code_info = calculate_code_info(code_nums_angles);

    add_candidates(candidates, code_info);
    const auto func = [&] {
        falgo::rotate_left(code_nums_angles);

        const auto code_info = calculate_code_info(code_numbers, code_angles);

        add_candidates(candidates, code_info);
    };

    falgo::repeat_n(func, code_nums_angles.size());

    falgo::reverse(code_nums_angles);

    add_candidates(candidates, code_info1);

    for (const auto& kv : candidates) {
        std::cout << kv << std::endl;
    }

    falgo::repeat_n(func, code_nums_angles.size());

    const auto inequalities = find_bad_angles(candidates);

    return inequalities;
}
