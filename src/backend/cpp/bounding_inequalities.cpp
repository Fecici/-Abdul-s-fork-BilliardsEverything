//#include "bad_angles.hpp"
#include "conversion.hpp"

#include "bounding_inequalities.hpp"

static std::vector<LinComArrZ<XYEtaPhi>> calculate_angles(const std::vector<std::pair<CodeNumber, XYZ>>& code_nums_angles) {

    std::vector<LinComArrZ<XYEtaPhi>> angles{};

    // phi = shooting_angle
    LinComArrZ<XYEtaPhi> shooting_angle{0, 0, 0, 1};

    angles.push_back(shooting_angle);

    LinComArrZ<XYEtaPhi> running_sum{};

    for (size_t i = 0; i < code_nums_angles.size() - 1; ++i) {
        running_sum.scale(-1);

        auto number = code_nums_angles.at(i).first;
        auto angle = code_nums_angles.at(i).second;

        running_sum.sub(number, xyz_to_xyetaphi(angle));

        LinComArrZ<XYEtaPhi> new_angle{};
        if (i % 2 == 0) {
            new_angle.add(2, XYEtaPhi::Eta); // Pi = 2*Eta
            new_angle.sub(XYEtaPhi::Phi);
            new_angle.add(running_sum);
        } else {
            new_angle.add(XYEtaPhi::Phi);
            new_angle.add(running_sum);
        }

        angles.push_back(new_angle);
    }

    return angles;
}

// each equation must be > 0
static std::array<LinComArrZ<XYEtaPhi>, 4> calculate_even_equations(const CodeNumber code_number, const XYZ code_angle, const LinComArrZ<XYEtaPhi>& theta) {

    // number = 2*n (even)
    CodeNumber n = code_number / 2;

    // 0 < theta
    LinComArrZ<XYEtaPhi> first{};
    first.add(theta);

    // theta + (n - 1)*angle < pi / 2
    // 0 < pi / 2 - theta - (n - 1)*angle
    LinComArrZ<XYEtaPhi> second{};
    second.add(XYEtaPhi::Eta);
    second.sub(theta);
    second.sub(n - 1, xyz_to_xyetaphi(code_angle));

    // pi / 2 < theta + (n + 1) * angle
    // 0 < theta + (n + 1) * angle - pi / 2
    LinComArrZ<XYEtaPhi> third{};
    third.add(theta);
    third.add(n + 1, xyz_to_xyetaphi(code_angle));
    third.sub(XYEtaPhi::Eta);

    // theta + (2n) *angle < pi
    // 0 < pi - theta - number*angle
    LinComArrZ<XYEtaPhi> fourth{};
    fourth.add(2, XYEtaPhi::Eta);
    fourth.sub(theta);
    fourth.sub(code_number, xyz_to_xyetaphi(code_angle));

    return {{first, second, third, fourth}};
}

static std::array<LinComArrZ<XYEtaPhi>, 4> calculate_odd_equations(const CodeNumber code_number, const XYZ code_angle, const LinComArrZ<XYEtaPhi>& theta) {

    // number = 2*n + 1 (odd)
    CodeNumber n = (code_number - 1) / 2;

    // 0 < theta
    LinComArrZ<XYEtaPhi> first{};
    first.add(theta);

    // theta + n*angle < pi / 2
    // 0 < pi / 2 - theta - n*angle
    LinComArrZ<XYEtaPhi> second{};
    second.add(XYEtaPhi::Eta);
    second.sub(theta);
    second.sub(n, xyz_to_xyetaphi(code_angle));

    // pi / 2 < theta + (n + 1)*angle
    // 0 < theta + (n + 1)*angle - pi / 2
    LinComArrZ<XYEtaPhi> third{};
    third.add(theta);
    third.add(n + 1, xyz_to_xyetaphi(code_angle));
    third.sub(XYEtaPhi::Eta);

    // theta + (2n + 1)*angle < pi
    // 0 < pi - theta - number*angle
    LinComArrZ<XYEtaPhi> fourth{};
    fourth.add(2, XYEtaPhi::Eta);
    fourth.sub(theta);
    fourth.sub(code_number, xyz_to_xyetaphi(code_angle));

    return {{first, second, third, fourth}};
}

static LinComArrZ<XYEta> remove_phi(const LinComArrZ<XYEtaPhi>& equation) {

    auto phi = equation.coeff<XYEtaPhi::Phi>();

    if (phi != 0) {
        throw std::runtime_error("remove_phi: non-zero phi coeff");
    }

    auto x = equation.coeff<XYEtaPhi::X>();
    auto y = equation.coeff<XYEtaPhi::Y>();
    auto eta = equation.coeff<XYEtaPhi::Eta>();

    return LinComArrZ<XYEta>{x, y, eta};
}

  /* 2025,jul,31
   * This function is updated to calcualte new code parallel at the same time
   */
static std::set<LinComArrZ<XYEta>> eliminate_phi(const std::set<LinComArrZ<XYEtaPhi>>& positive_phi, const std::set<LinComArrZ<XYEtaPhi>>& negative_phi) {
    unsigned int concurrency = std::thread::hardware_concurrency();
    if (concurrency == 0) concurrency = 4;

    // Convert positive_phi to vector for indexing/chunking
    std::vector<LinComArrZ<XYEtaPhi>> pos_vec(positive_phi.begin(), positive_phi.end());
    std::size_t n = pos_vec.size();

    // detect number of thread in computer
    std::size_t block_size;
    std::size_t task_num;
    if (n < 200) {
        block_size = (n + concurrency - 1) / concurrency;
        task_num = concurrency;
    } else {
        block_size = 1; 
        task_num = (n / block_size) + 1;
    }

    // Each thread accumulates in a chunked vector instead of a set to save RAM
    std::vector<std::vector<LinComArrZ<XYEta>>> thread_zero_phi(task_num);
    boost::asio::thread_pool pool(task_num);

    // Memory budget: ~24MB per thread
    const size_t MAX_BUFFER_SIZE = 1000000;

    for (unsigned int t = 0; t < task_num; ++t) {
        std::size_t begin = t * block_size;
        std::size_t end = std::min(begin + block_size, n);

        boost::asio::post(pool, [begin, end, t, &pos_vec, &negative_phi, &thread_zero_phi, MAX_BUFFER_SIZE] {
            
            std::vector<LinComArrZ<XYEta>> local_buffer;
            local_buffer.reserve(MAX_BUFFER_SIZE);

            for (std::size_t i = begin; i < end; ++i) {
                auto& positive_equation = pos_vec[i];
                for (const auto& negative_equation : negative_phi) {
                    
                    // Original math logic completely intact
                    auto zero_equation = LinComArrZ<XYEtaPhi>::add(positive_equation, negative_equation);
                    auto no_phi = remove_phi(zero_equation);
                    no_phi.divide_content();
                    
                    local_buffer.push_back(no_phi);

                    // Safety valve: clean up buffer when it gets too large
                    if (local_buffer.size() >= MAX_BUFFER_SIZE) {
                        std::sort(local_buffer.begin(), local_buffer.end());
                        auto last = std::unique(local_buffer.begin(), local_buffer.end());
                        local_buffer.erase(last, local_buffer.end());
                    }
                }
            }

            // Final clean up of remaining items in the buffer
            std::sort(local_buffer.begin(), local_buffer.end());
            auto last = std::unique(local_buffer.begin(), local_buffer.end());
            local_buffer.erase(last, local_buffer.end());

            thread_zero_phi[t] = std::move(local_buffer);
        });
    }

    pool.join();

    // Merge flat vectors back into the expected std::set return type
    std::set<LinComArrZ<XYEta>> zero_phi;
    for (unsigned int t = 0; t < task_num; ++t) {
        zero_phi.insert(thread_zero_phi[t].begin(), thread_zero_phi[t].end());
    }

    return zero_phi;
}

  /* 2025,jul,31
   * This function is updated to calcualte new code parallel at the same time
   */
static std::set<LinComArrZ<XYEta>> first_inequalities(const std::vector<std::pair<CodeNumber, XYZ>>& code_nums_angles) {

    auto theta_angles = calculate_angles(code_nums_angles);

    // assign max compuatation thread according to computer performence
    // if large set, small blocksize to allow time for memory swap
    unsigned int concurrency = std::thread::hardware_concurrency();
    if (concurrency == 0) concurrency = 4;
    std::size_t n = code_nums_angles.size();
    std::size_t block_size;
    std::size_t tasks_num;
    if (n<200){
        block_size = (n + concurrency - 1) / concurrency;
        tasks_num = concurrency;
    }else{
        block_size = 1; 
        tasks_num = (n/block_size)+1;
    }
    // use block size to serpate the word.
    

    boost::asio::thread_pool pool(concurrency);

    // We need this to be a set, since some of the equations can be duplicates
    std::vector<std::set<LinComArrZ<XYEtaPhi>>> thread_positive_phi(tasks_num);
    std::vector<std::set<LinComArrZ<XYEtaPhi>>> thread_negative_phi(tasks_num);

    // unfortunately, C++ doesn't have a zip feature, so
    // we have to just iterate
    for (unsigned int t = 0; t < tasks_num; ++t) {
        std::size_t begin = t * block_size;
        std::size_t end = std::min(begin + block_size, n);

        boost::asio::post(pool, [begin, end, t, &code_nums_angles, &theta_angles, &thread_positive_phi, &thread_negative_phi] {
            for (std::size_t i = begin; i < end; ++i) {
                auto code_number = code_nums_angles.at(i).first;
                auto code_angle = code_nums_angles.at(i).second;
                auto& theta = theta_angles.at(i);

                std::array<LinComArrZ<XYEtaPhi>, 4> equations;
                if (code_number % 2 == 0) {
                    equations = calculate_even_equations(code_number, code_angle, theta);
                } else {
                    equations = calculate_odd_equations(code_number, code_angle, theta);
                }

                for (auto& equation : equations) {
                    auto phi_coeff = equation.coeff<XYEtaPhi::Phi>();
                    if (phi_coeff == 1) {
                        thread_positive_phi[t].insert(equation);
                    } else if (phi_coeff == -1) {
                        thread_negative_phi[t].insert(equation);
                    } else {
                        std::ostringstream err{};
                        err << "phi_coeff " << phi_coeff << " is not 1 or -1";
                        throw std::runtime_error(err.str());
                    }
                }
            }
        });
    }

    pool.join();

    // Merge all thread sets into global sets
    std::set<LinComArrZ<XYEtaPhi>> positive_phi, negative_phi;
    for (unsigned int t = 0; t < tasks_num; ++t) {
        positive_phi.insert(thread_positive_phi[t].begin(), thread_positive_phi[t].end());
        negative_phi.insert(thread_negative_phi[t].begin(), thread_negative_phi[t].end());
    }

    auto no_phi_inequalities = eliminate_phi(positive_phi, negative_phi);

    return no_phi_inequalities;
}

// This doesn't really need to be sorted, but I like determinism.
std::set<LinComArrZ<XYEta>> calculate_bounding_inequalities(const std::vector<CodeNumber>& code_numbers, const std::vector<XYZ>& code_angles) {

    // nice little lambda to ensure const-correctness
    auto code_nums_angles = [&]() {
        std::vector<std::pair<CodeNumber, XYZ>> tmp;
        for (auto i : falgo::range(code_numbers.size())) {
            tmp.emplace_back(code_numbers.at(i), code_angles.at(i));
        }

        // Do it again
        if (tmp.size() % 2 != 0) {
            for (auto i : falgo::range(code_numbers.size())) {
                tmp.emplace_back(code_numbers.at(i), code_angles.at(i));
            }
        }

        return tmp;
    }();

    auto first_ineqs = first_inequalities(code_nums_angles);
    return first_ineqs;
    //const auto bad_angles = calculate_bad_angles(code_nums_angles);
    //std::cout << bad_angles << std::endl;

    //std::set<LinComArrZ<XYEta>> ineqs;
    //ineqs.insert(std::cbegin(first_ineqs), std::cend(first_ineqs));
    //std::cout << ineqs.size() << std::endl;
    //ineqs.insert(std::cbegin(bad_angles), std::cend(bad_angles));
    //std::cout << ineqs.size() << std::endl;

    //return ineqs;
}