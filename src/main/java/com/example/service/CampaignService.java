package com.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Service;

import com.example.enums.CampaignStatus;
import com.example.enums.DiscountType;
import com.example.model.Campaign;
import com.example.repository.CampaignRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CampaignService {

	@Autowired
	private CampaignRepository campaignRepository;

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public CampaignService(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<Campaign> findAll() {
		return campaignRepository.findAll();
	}

	public Optional<Campaign> findOne(Long id) {
		return campaignRepository.findById(id);
	}

	public Optional<Campaign> findByCode(String code) {
		return campaignRepository.findByCode(code);
	}

	@Transactional(readOnly = false)
	public Campaign save(Campaign entity) {
		return campaignRepository.save(entity);
	}

	@Transactional(readOnly = false)
	public void delete(Campaign entity) {
		campaignRepository.delete(entity);
	}

	/**
	 * CSVインポート処理
	 *
	 * @param file
	 * @throws IOException
	 */
	@Transactional
	public void importCSV(MultipartFile file) throws IOException {
		// try (BufferedReader br = new BufferedReader(
		// new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
		// String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
		// // TODO: ここを一括更新処理に変更したい batchInsertメソッドを使用するように
		// while ((line = br.readLine()) != null) {
		// final String[] split = line.replace("\"", "").split(",");
		// final Campaign campaign = new Campaign(
		// split[0], split[1], split[2], split[3],
		// DiscountType.valueOf(Integer.parseInt(split[4])),
		// CampaignStatus.valueOf(Integer.parseInt(split[5])), split[6]);
		// campaignRepository.save(campaign);
		// }
		// } catch (IOException e) {
		// throw new RuntimeException("ファイルが読み込めません", e);
		// }
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			String line = br.readLine(); // 1行目はヘッダーなので読み飛ばす
			List<Campaign> campaigns = new ArrayList<>();
			while ((line = br.readLine()) != null) {
				final String[] split = line.replace("\"", "").split(",");
				final Campaign campaign = new Campaign(
						split[0], split[1], split[2], split[3],
						DiscountType.valueOf(Integer.parseInt(split[4])),
						CampaignStatus.valueOf(Integer.parseInt(split[5])), split[6]);
				campaigns.add(campaign);
			}
			batchInsert(campaigns);
		} catch (IOException e) {
			throw new RuntimeException("ファイルが読み込めません", e);
		}
	}

	/**
	 * 一括更新処理実行
	 *
	 * @param campaigns
	 */
	@SuppressWarnings("unused")
	private int[] batchInsert(List<Campaign> campaigns) {
		String sql = "INSERT INTO campaigns (name, code, from_date, to_date, discount_type, status, description, create_at, update_at)"
				+ " VALUES(:name, :code, :from_date, :to_date, :discount_type, :status, :description, :create_at, :update_at)";
		// FIXME: ここでエラーが出る インサート文の問題？
		return jdbcTemplate.batchUpdate(sql,
				campaigns.stream()
						.map(c -> new MapSqlParameterSource()
								.addValue("name", c.getName(), Types.LONGVARCHAR)
								.addValue("code", c.getCode(), Types.LONGVARCHAR)
								.addValue("from_date", c.getFromDate(), Types.LONGVARCHAR)
								.addValue("to_date", c.getToDate(), Types.DATE)
								.addValue("discount_type", c.getDiscountType().getId(), Types.TINYINT)
								.addValue("status", c.getStatus().getId(), Types.TINYINT)
								.addValue("description", c.getDescription(), Types.LONGVARCHAR)
								.addValue("create_at", new Date(), Types.TIMESTAMP)
								.addValue("update_at", new Date(), Types.TIMESTAMP)) // update_atを設定する部分を追加
						.toArray(SqlParameterSource[]::new));
	}

	/**
	 * 一括ステータス更新処理 更新前にステータスをチェックする
	 *
	 * @param idList    更新対象IDリスト
	 * @param nexStatus 更新後ステータス
	 * @throws Exception
	 */
	@Transactional(readOnly = false, rollbackFor = RuntimeException.class)
	public void bulkStatusUpdate(List<Long> idList, CampaignStatus nexStatus) throws Exception {
		// try {
		// idList.forEach(id -> {
		// Campaign campaign = campaignRepository.findById(id).get();
		// // 更新前後のステータスが同じ場合はエラー
		// if (nexStatus.getId() == campaign.getStatus().getId()) {
		// throw new RuntimeException(campaign.getName() +
		// "にステータスの変更がないため、ステータスの一括更新に失敗しました。");
		// }
		// });
		// idList.forEach(id -> {
		// Campaign campaign = campaignRepository.findById(id).get();
		// campaign.setStatus(nexStatus);
		// campaignRepository.save(campaign);
		// });
		// } catch (RuntimeException e) {
		// throw new Exception(e.getMessage());
		// }
		boolean allStatusMatch = true; // フラグを初期化

		try {
			for (Long id : idList) {
				Campaign campaign = campaignRepository.findById(id).get();
				// 更新前後のステータスが同じ場合はフラグを false にしてループを終了
				if (nexStatus.getId() == campaign.getStatus().getId()) {
					allStatusMatch = false;
					break;
				}
			}

			if (allStatusMatch) {
				// すべてのステータスが一致しない場合のみ、すべてのステータスを更新
				for (Long id : idList) {
					Campaign campaign = campaignRepository.findById(id).get();
					campaign.setStatus(nexStatus);
					campaignRepository.save(campaign);
				}
			} else {
				// 一つでも一致するステータスがある場合はエラーをスロー
				throw new RuntimeException("ステータスの変更がないため、ステータスの一括更新に失敗しました。");
			}
		} catch (RuntimeException e) {
			throw new Exception(e.getMessage());
		}
	}

}
